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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void finalizaMarcandoComoAtendida(){
        existeNoBanco(StatusSenha.CHAMADA);

        Senha finalizada = service.finalizar(1L);

        assertEquals(StatusSenha.ATENDIDA, finalizada.getStatus());
        assertNotNull(finalizada.getFinalizadaEm());
    }

    @Test
    void marcaAusente(){
        existeNoBanco(StatusSenha.CHAMADA);

        Senha ausente = service.marcarAusente(1L);

        assertEquals(StatusSenha.AUSENTE, ausente.getStatus());
        assertNotNull(ausente.getFinalizadaEm());
    }

    @Test
    void rechamaAtualizandoHorarioDaChamada(){
        Senha senha = existeNoBanco(StatusSenha.CHAMADA);
        LocalDateTime primeiraChamada = LocalDateTime.now().minusMinutes(5);
        senha.setChamadaEm(primeiraChamada);

        Senha rechamada = service.rechamar(1L);

        assertEquals(StatusSenha.CHAMADA, rechamada.getStatus());
        assertTrue(rechamada.getChamadaEm().isAfter(primeiraChamada));
    }

    @Test
    void naoFinalizaSenhaJaAtendida(){
        existeNoBanco(StatusSenha.ATENDIDA);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.finalizar(1L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void naoMarcaAusenteSenhaAguardando(){
        existeNoBanco(StatusSenha.AGUARDANDO);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.marcarAusente(1L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void naoRechamaSenhaInexistente(){
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.rechamar(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    private Senha existeNoBanco(StatusSenha status){
        Senha senha = novaSenha(TipoSenha.NORMAL);
        senha.setStatus(status);
        when(repository.findById(1L)).thenReturn(Optional.of(senha));
        return senha;
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