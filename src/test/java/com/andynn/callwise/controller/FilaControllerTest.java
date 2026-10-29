package com.andynn.callwise.controller;

import com.andynn.callwise.dto.SenhaResponse;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.StatusSenha;
import com.andynn.callwise.model.TipoSenha;
import com.andynn.callwise.service.FilaService;
import com.andynn.callwise.service.PainelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaControllerTest {

    @Mock
    private FilaService service;

    @Mock
    private PainelService painelService;

    private FilaController controller;

    @BeforeEach
    void preparar(){
        controller = new FilaController(service, painelService);
    }

    @Test
    void anunciaNoPainelASenhaChamada(){
        Senha senha = novaSenha(StatusSenha.CHAMADA);
        when(service.chamarProxima()).thenReturn(Optional.of(senha));

        ResponseEntity<SenhaResponse> resposta = controller.chamarProxima();

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        verify(painelService).anunciar(resposta.getBody());
    }

    @Test
    void naoAnunciaNadaComFilaVazia(){
        when(service.chamarProxima()).thenReturn(Optional.empty());

        ResponseEntity<SenhaResponse> resposta = controller.chamarProxima();

        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        verifyNoInteractions(painelService);
    }

    @Test
    void anunciaNoPainelASenhaRechamada(){
        Senha senha = novaSenha(StatusSenha.CHAMADA);
        when(service.rechamar(1L)).thenReturn(senha);

        SenhaResponse resposta = controller.rechamar(1L);

        verify(painelService).anunciar(resposta);
    }

    @Test
    void naoAnunciaAoFinalizar(){
        Senha senha = novaSenha(StatusSenha.ATENDIDA);
        when(service.finalizar(1L)).thenReturn(senha);

        controller.finalizar(1L);

        verifyNoInteractions(painelService);
    }

    private Senha novaSenha(StatusSenha status){
        Senha senha = new Senha();
        senha.setId(1L);
        senha.setTipo(TipoSenha.NORMAL);
        senha.setCodigo("N001");
        senha.setStatus(status);
        return senha;
    }
}
