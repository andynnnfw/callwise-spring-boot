package com.andynn.callwise.service;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PainelService {
    private final SimpMessagingTemplate messagingTemplate;
}
