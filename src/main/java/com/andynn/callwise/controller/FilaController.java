package com.andynn.callwise.controller;

import com.andynn.callwise.dto.SenhaResponse;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.service.FilaService;
import com.andynn.callwise.service.SenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/fila")
@RequiredArgsConstructor
public class FilaController {
    private final FilaService service;

    @PostMapping("/chamar")
    public ResponseEntity<SenhaResponse> chamarProxima(){
        Optional<Senha> senha = service.chamarProxima();
        if (senha.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(SenhaResponse.de(senha.get()));
    }

    @PostMapping("/{id}/finalizar")
    public SenhaResponse finalizar(@PathVariable Long id){
        return SenhaResponse.de(service.finalizar(id));
    }

    @PostMapping("/{id}/ausente")
    public SenhaResponse marcarAusente(@PathVariable Long id){
        return SenhaResponse.de(service.marcarAusente(id));
    }

    @PostMapping("/{id}/rechamar")
    public SenhaResponse rechamar(@PathVariable Long id){
        return SenhaResponse.de(service.rechamar(id));
    }
}
