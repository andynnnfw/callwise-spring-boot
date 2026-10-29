package com.andynn.callwise.dto;

import com.andynn.callwise.model.TipoSenha;
import jakarta.validation.constraints.NotNull;

public record EmitirSenhaRequest(@NotNull(message = "Informe o tipo da senha: NORMAL ou PRIORITARIA") TipoSenha tipo) {
}
