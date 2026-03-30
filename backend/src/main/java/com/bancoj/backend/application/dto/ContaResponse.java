package com.bancoj.backend.application.dto;

import com.bancoj.backend.domain.model.Conta;
import lombok.Data;

@Data
public class ContaResponse {
    private long id;
    private int numero;
    private String titular;
    private double saldo;
    private String tipo;

    public static ContaResponse from(Conta conta) {
        ContaResponse r = new ContaResponse();
        r.id = conta.getId();
        r.numero = conta.getNumero();
        r.titular = conta.getTitular();
        r.saldo = conta.getSaldo();
        return r;
    }
}
