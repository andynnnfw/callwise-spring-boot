package com.andynn.callwise.service;

import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.StatusSenha;
import com.andynn.callwise.model.TipoSenha;
import com.andynn.callwise.repository.SenhaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaServiceTest {

    @Mock
    private SenhaRepository repository;

    private FilaService service;

    @BeforeEach
    void preparar(){
        service = new FilaService(repository, new RegraFila());
    }

    @Test
    void naoChamaNinguemComFilaVazia(){
        Optional<Senha> chamada = service.chamarProxima();

        assertTrue(chamada.isEmpty());
    }

    @Test
    void chamaNormalEMarcaComoChamada(){
        temAguardando(TipoSenha.NORMAL);
        temAguardando(TipoSenha.PRIORITARIA);
        proximaDaFila(TipoSenha.NORMAL);

        Senha chamada = service.chamarProxima().orElseThrow();

        assertEquals(TipoSenha.NORMAL, chamada.getTipo());
        assertEquals(StatusSenha.CHAMADA, chamada.getStatus());
        assertNotNull(chamada.getChamadaEm());
    }

    @Test
    void chamaPrioritariaDepoisDeDuasNormais(){
        temAguardando(TipoSenha.NORMAL);
        temAguardando(TipoSenha.PRIORITARIA);
        when(repository.findTop2ByDataEmissaoAndChamadaEmIsNotNullOrderByChamadaEmDesc(any()))
                .thenReturn(List.of(novaSenha(TipoSenha.NORMAL), novaSenha(TipoSenha.NORMAL)));
        proximaDaFila(TipoSenha.PRIORITARIA);

        Senha chamada = service.chamarProxima().orElseThrow();

        assertEquals(TipoSenha.PRIORITARIA, chamada.getTipo());
        assertEquals(StatusSenha.CHAMADA, chamada.getStatus());
    }


    private Senha novaSenha(TipoSenha tipo){
        Senha senha = new Senha();
        senha.setTipo(tipo);
        return senha;
    }

    private void temAguardando(TipoSenha tipo){
        when(repository.existsByTipoAndStatusAndDataEmissao(eq(tipo), eq(StatusSenha.AGUARDANDO), any()))
                .thenReturn(true);
    }

    private void proximaDaFila(TipoSenha tipo){
        when(repository.findFirstByTipoAndStatusAndDataEmissaoOrderByNumeroAsc(eq(tipo), eq(StatusSenha.AGUARDANDO), any()))
                .thenReturn(Optional.of(novaSenha(tipo)));
    }
}