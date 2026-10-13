package com.andynn.callwise.service;


import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.StatusSenha;
import com.andynn.callwise.model.TipoSenha;
import com.andynn.callwise.repository.SenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FilaService {
    private final SenhaRepository repository;
    private  final RegraFila regraFila;

    @Transactional
    public Optional<Senha> chamarProxima(){
        LocalDate hoje = LocalDate.now();

        boolean temNormal = repository.existsByTipoAndStatusAndDataEmissao(TipoSenha.NORMAL, StatusSenha.AGUARDANDO, hoje);
        boolean temPrioritaria = repository.existsByTipoAndStatusAndDataEmissao(TipoSenha.PRIORITARIA, StatusSenha.AGUARDANDO, hoje);
        int normaisSeguidas = contarNormaisSeguidas(hoje);

        Optional<TipoSenha> tipo = regraFila.proximoTipo(normaisSeguidas, temNormal, temPrioritaria);
        if (tipo.isEmpty()){
            return Optional.empty();
        }

        Senha senha = repository.findFirstByTipoAndStatusAndDataEmissaoOrderByNumeroAsc(tipo.get(),
                StatusSenha.AGUARDANDO, hoje).orElseThrow();
                senha.setStatus(StatusSenha.CHAMADA);
                senha.setChamadaEm(LocalDateTime.now());
                return Optional.of(senha);
    }

    private int contarNormaisSeguidas(LocalDate hoje) {
        int normais = 0;
        for(Senha senha : repository.findTop2ByDataEmissaoAndChamadaEmIsNotNullOrderByChamadaEmDesc(hoje)){
            if (senha.getTipo() != TipoSenha.NORMAL){
                break;
            }
            normais++;
        }
        return normais;
    }
}
