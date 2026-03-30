package com.bancoj.backend.api.controller;

import com.bancoj.backend.application.dto.*;
import com.bancoj.backend.application.service.ContaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contas")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContaResponse>> criar(
            @Valid @RequestBody CriarContaRequest req) {
        ContaResponse conta = contaService.criarConta(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Conta criada com sucesso.", conta));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Contas recuperadas.", contaService.listarTodas()));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<ApiResponse<ContaResponse>> buscar(@PathVariable Integer numero) {
        return ResponseEntity.ok(ApiResponse.ok("Conta encontrada.", contaService.buscarPorNumero(numero)));
    }

    @PostMapping("/depositar")
    public ResponseEntity<ApiResponse<ContaResponse>> depositar(
            @Valid @RequestBody DepositoSaqueRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Depósito realizado.", contaService.depositar(req)));
    }

    @PostMapping("/sacar")
    public ResponseEntity<ApiResponse<ContaResponse>> sacar(
            @Valid @RequestBody DepositoSaqueRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Saque realizado.", contaService.sacar(req)));
    }

    @PostMapping("/transferir")
    public ResponseEntity<ApiResponse<Void>> transferir(
            @Valid @RequestBody TransferenciaRequest req) {
        contaService.transferir(req);
        return ResponseEntity.ok(ApiResponse.ok("Transferência realizada."));
    }

    @PostMapping("/{numero}/rendimento")
    public ResponseEntity<ApiResponse<ContaResponse>> rendimento(@PathVariable Integer numero) {
        return ResponseEntity.ok(ApiResponse.ok("Rendimento/taxa aplicado.", contaService.aplicarRendimento(numero)));
    }

    @GetMapping("/{numero}/historico")
    public ResponseEntity<ApiResponse<List<HistoricoResponse>>> historico(@PathVariable Integer numero) {
        return ResponseEntity.ok(ApiResponse.ok("Histórico recuperado.", contaService.historico(numero)));
    }

    @GetMapping("/tributos")
    public ResponseEntity<ApiResponse<TributoResponse>> tributos() {
        return ResponseEntity.ok(ApiResponse.ok("Tributos calculados.", contaService.calcularTributos()));
    }

    @GetMapping("/top-saldos")
    public ResponseEntity<ApiResponse<List<ContaResponse>>> topSaldos() {
        return ResponseEntity.ok(ApiResponse.ok("Ranking de saldos.", contaService.topSaldos()));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<ApiResponse<List<ContaResponse>>> filtrar(
            @RequestParam Double saldoMinimo) {
        return ResponseEntity.ok(ApiResponse.ok("Contas filtradas.", contaService.filtrarPorSaldo(saldoMinimo)));
    }

    @DeleteMapping("/{numero}")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer numero) {
        contaService.excluirConta(numero);
        return ResponseEntity.ok(ApiResponse.ok("Conta excluída com sucesso."));
    }
}
