package com.andynn.callwise.service;

import com.andynn.callwise.model.TipoSenha;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegraFilaTest {

    private final RegraFila regra = new RegraFila();

    @Test
    void chamaPrioritariaDepoisDeDuasNormais(){
        assertEquals(Optional.of(TipoSenha.PRIORITARIA), regra.proximoTipo(2, true, true));
    }

    @Test
    void chamaNormalAntesDeCompletarDuas(){
        assertEquals(Optional.of(TipoSenha.NORMAL), regra.proximoTipo(0, true, true));
        assertEquals(Optional.of(TipoSenha.NORMAL), regra.proximoTipo(1, true, true));
    }

    @Test
    void chamaNormalSeNaoTemPrioritaria(){
        assertEquals(Optional.of(TipoSenha.NORMAL), regra.proximoTipo(2, true, false));
    }

    @Test
    void chamaPrioritariaQueChegouNoMeio(){
        assertEquals(Optional.of(TipoSenha.PRIORITARIA), regra.proximoTipo(3, true , true));
    }

    @Test
    void chamaPrioritariaSeNaoTemNormal(){
        assertEquals(Optional.of(TipoSenha.PRIORITARIA), regra.proximoTipo(0, false, true));
    }

    @Test
    void naoChamaNinguemComFilaVazia(){
        assertEquals(Optional.empty(), regra.proximoTipo(0, false, false));
    }

}
