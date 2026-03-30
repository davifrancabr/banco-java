package com.bancoj.backend.application.dto;

import com.bancoj.backend.domain.model.HistoricoOperacao;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoricoResponse {
    private long id;
    private String descricao;
    private LocalDateTime dataHora;

    public static HistoricoResponse from(HistoricoOperacao h) {
        HistoricoResponse r = new HistoricoResponse();
        r.id = h.getId();
        r.descricao = h.getDescricao();
        r.dataHora = h.getDataHora();
        return r;
    }
}
