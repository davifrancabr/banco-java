package com.bancoj.backend.exception;

public class ContaNaoEncontradaException extends RuntimeException {
    public ContaNaoEncontradaException(int numero) {
        super("Conta não encontrada: " + numero);
    }
}
