package com.bancoj.backend.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CORRENTE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ContaCorrente extends Conta {
    private static final double TAXA_MANUTENCAO = 20.0;
    private static final double ALIQUOTA_TRIBUTO = 0.005;

    public ContaCorrente(Integer numero, String titular, Double saldo) {
        super(numero,titular,saldo);
    }

    @Override
    public double calcularRendimento() {
        if (getSaldo() < TAXA_MANUTENCAO) return 0;
        setSaldo(getSaldo() - TAXA_MANUTENCAO);
        return -TAXA_MANUTENCAO;
    }

    @Override
    public double calcularTributo() {
        return getSaldo() * ALIQUOTA_TRIBUTO;
    }

    @Override
    public String getTipoDescricao() {
        return "Corrente";
    }
}
