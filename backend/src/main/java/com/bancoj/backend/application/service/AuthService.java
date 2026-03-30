package com.bancoj.backend.application.service;

import com.bancoj.backend.application.dto.LoginRequest;
import com.bancoj.backend.application.dto.LoginResponse;
import com.bancoj.backend.domain.model.Funcionario;
import com.bancoj.backend.exception.OperacaoInvalidaException;
import com.bancoj.backend.infrastructure.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest req) {
        Funcionario func = funcionarioRepository.findByNome(req.getNome())
                .orElseThrow(()-> new OperacaoInvalidaException(("Credenciais inválidas.")));

        if (!passwordEncoder.matches(req.getSenha(), func.getSenha())) {
            throw new OperacaoInvalidaException("Credenciais inválidas.");
        }

        return new LoginResponse(func.getNome(), func.getCargo(), func.getAgencia());
    }
}
