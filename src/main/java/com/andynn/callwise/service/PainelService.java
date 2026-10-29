package com.andynn.callwise.service;


import com.andynn.callwise.dto.SenhaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PainelService {
    private final SimpMessagingTemplate messagingTemplate;

    public void anunciar(SenhaResponse senha){
        messagingTemplate.convertAndSend("/topic/painel", senha);
    }
}
