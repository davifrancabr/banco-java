package com.bancoj.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String nome;
    private String cargo;
    private String agencia;
}
