package com.andynn.callwise.controller;

import com.andynn.callwise.dto.EmitirSenhaRequest;
import com.andynn.callwise.dto.SenhaResponse;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.service.SenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/senhas")
@RequiredArgsConstructor
public class SenhaController {
    private final SenhaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SenhaResponse emitir(@RequestBody @Valid EmitirSenhaRequest request){
        Senha senha = service.emitir(request.tipo());
        return SenhaResponse.de(senha);
    }
}
