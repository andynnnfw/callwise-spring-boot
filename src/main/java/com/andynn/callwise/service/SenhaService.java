package com.andynn.callwise.service;

import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.TipoSenha;
import com.andynn.callwise.repository.SenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SenhaService {
    private final SenhaRepository repository;

    @Transactional
    public Senha emitir(TipoSenha tipo){
        LocalDate hoje = LocalDate.now();
        int proximo = repository.ultimoNumeroDoDia(tipo, hoje) + 1;
        Senha senha = new Senha();
        senha.setTipo(tipo);
        senha.setNumero(proximo);
        senha.setDataEmissao(hoje);
        senha.setEmitidaEm(LocalDateTime.now());
        senha.setCodigo(tipo.getPrefixo() + String.format("%03d", proximo));
        return repository.save(senha);
    }
}
