package com.bancoj.backend.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("POUPANCA")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ContaPoupanca extends Conta {
    private static final double TAXA_RENDIMENTO = 0.05;

    public ContaPoupanca(Integer numero, String titular, Double saldo) {
        super(numero, titular, saldo);
    }

    @Override
    public double calcularRendimento() {
        double rendimento = getSaldo() * TAXA_RENDIMENTO;
        setSaldo(getSaldo() + rendimento);
        return rendimento;
    }

    @Override
    public double calcularTributo() {
        return 0.0;
    }

    @Override
    public String getTipoDescricao() {
        return "Conta Poupança";
    }
}
