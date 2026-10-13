package com.andynn.callwise.service;

import com.andynn.callwise.model.TipoSenha;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RegraFila {
    public Optional<TipoSenha> proximoTipo(int normaisSeguidas, boolean temNormal, boolean temPrioritaria){
        if (!temNormal && !temPrioritaria){
            return Optional.empty();
        }
        if (temPrioritaria && (normaisSeguidas >= 2 || !temNormal)){
            return Optional.of(TipoSenha.PRIORITARIA);
        }
        return Optional.of(TipoSenha.NORMAL);
    }
}
