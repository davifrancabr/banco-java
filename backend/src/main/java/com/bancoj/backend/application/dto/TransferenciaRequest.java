package com.bancoj.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferenciaRequest {

    @NotNull(message = "A conta de origem é obrigatória.")
    private Integer contaOrigem;

    @NotNull(message = "A conta de destino é obrigatória.")
    private Integer contaDestino;

    @NotNull(message = "O valor é obrigatório.")
    @Positive(message = "O valor deve ser positivo.")
    private Double valor;
}
