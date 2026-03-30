package com.bancoj.backend.exception;

public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(double saldoAtual) {
        super(String.format("Saldo insuficiente. Saldo atual: R$ %.2f", saldoAtual));
    }
}
