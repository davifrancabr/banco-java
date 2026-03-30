package com.bancoj.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CriarContaRequest {

    @NotBlank(message = "O nome do titular é obrigatório.")
    private String titular;

    @NotNull(message = "O saldo inicial é obrigatório.")
    @PositiveOrZero(message = "O sado inicial não pode ser negativo.")
    private Double saldoInicial;

    @NotNull(message = "O tipo de conta é obrigatório.")
    @Pattern(regexp = "CORRENTE|POUPANCA", message = "Tipo deve ser 'CORRENTE' ou 'POUPANCA'.")
    private String tipo;
}
