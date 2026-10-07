package com.andynn.callwise.dto;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.StatusSenha;
import com.andynn.callwise.model.TipoSenha;
import java.time.LocalDateTime;

public record SenhaResponse(Long id, String codigo, TipoSenha tipo, StatusSenha status, LocalDateTime emitidaEm) {
    public static SenhaResponse de (Senha s){
        return new SenhaResponse(s.getId(), s.getCodigo(), s.getTipo(), s.getStatus(), s.getEmitidaEm());
    }
}