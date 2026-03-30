package com.bancoj.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean sucesso;
    private String mensagem;
    private T dados;

    public static <T> ApiResponse<T> ok(String mensagem, T dados) {
        return new ApiResponse<>(true, mensagem, dados);
    }

    public static <T> ApiResponse<T> ok(String mensagem) {
        return new ApiResponse<>(true, mensagem, null);
    }

    public static <T> ApiResponse<T> erro(String mensagem) {
        return new ApiResponse<>(false, mensagem, null);
    }
}
