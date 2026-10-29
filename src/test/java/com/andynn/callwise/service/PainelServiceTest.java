package com.andynn.callwise.service;

import com.andynn.callwise.dto.SenhaResponse;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.TipoSenha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PainelServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private PainelService service;

    @BeforeEach
    void preparar(){
        service = new PainelService(messagingTemplate);
    }

    @Test
    void anunciaSenhaNoTopicoDoPainel(){
        Senha senha = new Senha();
        senha.setTipo(TipoSenha.NORMAL);
        senha.setCodigo("N001");
        SenhaResponse response = SenhaResponse.de(senha);

        service.anunciar(response);

        verify(messagingTemplate).convertAndSend("/topic/painel", response);
    }
}
