package com.andynn.callwise.service;

import com.andynn.callwise.model.TipoSenha;

import java.util.Optional;

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
