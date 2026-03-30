package com.bancoj.backend.infrastructure.repository;


import com.bancoj.backend.domain.model.HistoricoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoOperacaoRepository extends JpaRepository<HistoricoOperacao, Long> {
    List<HistoricoOperacao> findByContaNumeroOrderByDataHoraAsc(Integer numero);
}
