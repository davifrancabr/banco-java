package com.bancoj.backend.application.service;

import com.bancoj.backend.application.dto.*;
import com.bancoj.backend.domain.model.Conta;
import com.bancoj.backend.domain.model.ContaCorrente;
import com.bancoj.backend.domain.model.ContaPoupanca;
import com.bancoj.backend.domain.model.HistoricoOperacao;
import com.bancoj.backend.exception.ContaNaoEncontradaException;
import com.bancoj.backend.exception.OperacaoInvalidaException;
import com.bancoj.backend.exception.SaldoInsuficienteException;
import com.bancoj.backend.infrastructure.repository.ContaRepository;
import com.bancoj.backend.infrastructure.repository.HistoricoOperacaoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaService {
    private final ContaRepository contaRepository;
    private final HistoricoOperacaoRepository historicoRepository;

    @Transactional
    public ContaResponse criarConta(CriarContaRequest req) {
        int proximoNumero = contaRepository.findMaxNumero() + 1;
        Conta conta = switch (req.getTipo()) {
            case "CORRENTE" -> new ContaCorrente(proximoNumero, req.getTitular(), req.getSaldoInicial());
            case "POUPANCA" -> new ContaPoupanca(proximoNumero, req.getTitular(), req.getSaldoInicial());
            default -> throw new OperacaoInvalidaException("Tipo inválido: " + req.getTipo());
        };

        conta = contaRepository.save(conta);
        registrar(conta, "Conta criada com saldo inicial de R$ " + String.format("%.2f", req.getSaldoInicial()));

        return ContaResponse.from(conta);
    }

    public List<ContaResponse> listarTodas() {
        return contaRepository.findAll()
                .stream().map(ContaResponse::from).toList();
    }

    public ContaResponse buscarPorNumero(Integer numero) {
        return ContaResponse.from(encontrar(numero));
    }

    @Transactional
    public ContaResponse depositar(DepositoSaqueRequest req) {
        Conta conta = encontrar(req.getNumeroConta());
        conta.depositar(req.getValor());
        contaRepository.save(conta);
        registrar(conta, "Depósito de R$ " + fmt(req.getValor()));
        return ContaResponse.from(conta);
    }

    @Transactional
    public ContaResponse sacar(DepositoSaqueRequest req) {
        Conta conta = encontrar(req.getNumeroConta());

        if (!conta.sacar(req.getValor())) throw new SaldoInsuficienteException(conta.getSaldo());

        contaRepository.save(conta);
        registrar(conta, "Saque de R$ " + fmt(req.getValor()));
        return ContaResponse.from(conta);
    }

    @Transactional
    public void transferir(TransferenciaRequest req) {
        if (req.getContaOrigem().equals(req.getContaDestino()))
            throw new OperacaoInvalidaException("Conta de origem e destino não podem ser iguais.");

        Conta origem = encontrar(req.getContaOrigem());
        Conta destino = encontrar(req.getContaDestino());

        if (!origem.sacar(req.getValor()))
            throw new SaldoInsuficienteException(origem.getSaldo());

        destino.depositar(req.getValor());
        contaRepository.save(origem);
        contaRepository.save(destino);

        registrar(origem,  "Transferência enviada de R$ "   + fmt(req.getValor())
                + " para conta "   + destino.getNumero() + " (" + destino.getTitular() + ")");
        registrar(destino, "Transferência recebida de R$ "  + fmt(req.getValor())
                + " da conta "     + origem.getNumero()  + " (" + origem.getTitular()  + ")");
    }

    @Transactional
    public ContaResponse aplicarRendimento(Integer numero) {
        Conta conta = encontrar(numero);
        double delta = conta.calcularRendimento();

        if (delta == 0 && conta instanceof ContaCorrente)
            throw new SaldoInsuficienteException(conta.getSaldo());

        conta.setSaldo(conta.getSaldo() - delta);

        contaRepository.save(conta);

        String msg = delta >= 0
                ? "Rendimento de 5% aplicado: R$ " + fmt(delta)
                : "Taxa de manutenção descontada: R$ " + fmt(Math.abs(delta));

        return ContaResponse.from(conta);
    }

    public TributoResponse calcularTributos() {
        List<TributoResponse.ItemTributo> itens = contaRepository.findAll().stream()
                .filter(c -> c.calcularTributo() > 0)
                .map(c -> new TributoResponse.ItemTributo(
                        c.getNumero(), c.getTitular(), c.getSaldo(), c.calcularTributo()
                ))
                .toList();
        double total = itens.stream().mapToDouble(TributoResponse.ItemTributo::getTributo).sum();
        return new TributoResponse(itens, total, itens.size());
    }

    public List<HistoricoResponse> historico(Integer numero) {
        encontrar(numero);
        return historicoRepository.findByContaNumeroOrderByDataHoraAsc(numero)
                .stream().map(HistoricoResponse::from).toList();
    }

    public List<ContaResponse> topSaldos() {
        return contaRepository.findAllOrderBySaldoDesc()
                .stream().map(ContaResponse::from).toList();
    }

    public List<ContaResponse> filtrarPorSaldo(double saldoMinimo) {
        return contaRepository.findBySaldoGreaterThanEqual(saldoMinimo)
                .stream().map(ContaResponse::from).toList();
    }

    @Transactional
    public void excluirConta(Integer numero) {
        Conta conta = encontrar(numero);
        contaRepository.delete(conta);
    }

    private Conta encontrar(Integer numero) {
        return contaRepository.findByNumero(numero)
                .orElseThrow(() -> new ContaNaoEncontradaException(numero));
    }

    private void registrar(Conta conta, String descricao) {
        HistoricoOperacao h = HistoricoOperacao.builder()
                .conta(conta)
                .descricao(descricao)
                .build();
        h.prePersist();
        historicoRepository.save(h);
    }

    private String fmt(double valor) {
        return String.format("%.2f", valor);
    }
}
