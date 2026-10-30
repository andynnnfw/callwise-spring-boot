package com.andynn.callwise.exception;

import com.andynn.callwise.config.SecurityConfig;
import com.andynn.callwise.controller.FilaController;
import com.andynn.callwise.controller.SenhaController;
import com.andynn.callwise.service.FilaService;
import com.andynn.callwise.service.PainelService;
import com.andynn.callwise.service.SenhaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({FilaController.class, SenhaController.class})
@Import(SecurityConfig.class)
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FilaService filaService;

    @MockitoBean
    private PainelService painelService;

    @MockitoBean
    private SenhaService senhaService;

    @Test
    void senhaInexistenteRetorna404ComMensagem() throws Exception {
        when(filaService.finalizar(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Senha não encontrada"));

        mvc.perform(post("/api/fila/99/finalizar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Senha não encontrada"))
                .andExpect(jsonPath("$.caminho").value("/api/fila/99/finalizar"));
    }

    @Test
    void senhaForaDeAtendimentoRetorna409ComMensagem() throws Exception {
        when(filaService.marcarAusente(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Esta senha não está em atendimento"));

        mvc.perform(post("/api/fila/1/ausente"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Esta senha não está em atendimento"))
                .andExpect(jsonPath("$.caminho").value("/api/fila/1/ausente"));
    }

    @Test
    void senhaSemTipoRetorna400ComMensagemDaValidacao() throws Exception {
        mvc.perform(post("/api/senhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Informe o tipo da senha: NORMAL ou PRIORITARIA"))
                .andExpect(jsonPath("$.caminho").value("/api/senhas"));

        verifyNoInteractions(senhaService);
    }

    @Test
    void tipoDeSenhaInexistenteRetorna400() throws Exception {
        mvc.perform(post("/api/senhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VIP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem")
                        .value("Corpo da requisição inválido. Confira o JSON e se o tipo é NORMAL ou PRIORITARIA"))
                .andExpect(jsonPath("$.caminho").value("/api/senhas"));

        verifyNoInteractions(senhaService);
    }

    @Test
    void idQueNaoENumeroRetorna400() throws Exception {
        mvc.perform(post("/api/fila/abc/finalizar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para id: abc"))
                .andExpect(jsonPath("$.caminho").value("/api/fila/abc/finalizar"));

        verifyNoInteractions(filaService);
    }

    @Test
    void erroInesperadoRetorna500SemExporDetalhes() throws Exception {
        when(filaService.rechamar(1L)).thenThrow(new RuntimeException("falha interna no banco"));

        mvc.perform(post("/api/fila/1/rechamar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensagem").value("Erro interno inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.caminho").value("/api/fila/1/rechamar"));

        verifyNoInteractions(painelService);
    }
}
