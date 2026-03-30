package com.bancoj.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TributoResponse {
    private List<ItemTributo> contas;
    private Double totalTributos;
    private Integer totalContas;

    @Data
    @AllArgsConstructor
    public static class ItemTributo {
        private Integer numero;
        private String titular;
        private Double saldo;
        private Double tributo;
    }
}
