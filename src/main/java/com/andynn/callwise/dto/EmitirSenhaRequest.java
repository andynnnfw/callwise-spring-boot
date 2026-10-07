package com.andynn.callwise.dto;

import com.andynn.callwise.model.TipoSenha;
import jakarta.validation.constraints.NotNull;

public record EmitirSenhaRequest(@NotNull TipoSenha tipo) {
}
