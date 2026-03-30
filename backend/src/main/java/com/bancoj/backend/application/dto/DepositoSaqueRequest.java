package com.bancoj.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DepositoSaqueRequest {

    @NotNull(message = "O número da conta é obrigatório.")
    private int numeroConta;

    @NotNull(message = "O valor é obrigatório.")
    @Positive(message = "O valor deve ser positivo.")
    private double valor;
}
