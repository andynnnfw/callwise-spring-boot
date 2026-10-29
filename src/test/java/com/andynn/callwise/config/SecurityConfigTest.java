package com.andynn.callwise.config;

import com.andynn.callwise.controller.FilaController;
import com.andynn.callwise.controller.SenhaController;
import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.TipoSenha;
import com.andynn.callwise.service.FilaService;
import com.andynn.callwise.service.PainelService;
import com.andynn.callwise.service.SenhaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({FilaController.class, SenhaController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.user.name=atendente-teste",
        "spring.security.user.password=senha-teste"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FilaService filaService;

    @MockitoBean
    private PainelService painelService;

    @MockitoBean
    private SenhaService senhaService;

    @Test
    void totemPainelELoginSaoPublicos() throws Exception {
        mvc.perform(get("/totem/index.html")).andExpect(status().isOk());
        mvc.perform(get("/painel/index.html")).andExpect(status().isOk());
        mvc.perform(get("/login/index.html")).andExpect(status().isOk());
    }

    @Test
    void retirarSenhaNaoExigeLogin() throws Exception {
        Senha senha = new Senha();
        senha.setTipo(TipoSenha.NORMAL);
        senha.setCodigo("N001");
        when(senhaService.emitir(TipoSenha.NORMAL)).thenReturn(senha);

        mvc.perform(post("/api/senhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"NORMAL\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void telaDoAtendenteSemLoginVaiParaOLogin() throws Exception {
        mvc.perform(get("/atendente/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/index.html"));
    }

    @Test
    void chamarSenhaSemLoginNaoChegaNoServico() throws Exception {
        mvc.perform(post("/api/fila/chamar"))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(filaService);
    }

    @Test
    @WithMockUser
    void atendenteLogadoConsegueChamar() throws Exception {
        when(filaService.chamarProxima()).thenReturn(Optional.empty());

        mvc.perform(post("/api/fila/chamar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginCertoVaiParaATelaDoAtendente() throws Exception {
        mvc.perform(formLogin("/login").user("atendente-teste").password("senha-teste"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/atendente/index.html"));
    }

    @Test
    void loginErradoVoltaComAvisoDeErro() throws Exception {
        mvc.perform(formLogin("/login").user("atendente-teste").password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login/index.html?erro"));
    }

    @Test
    @WithMockUser
    void sairVoltaParaOLogin() throws Exception {
        mvc.perform(logout())
                .andExpect(redirectedUrl("/login/index.html?saiu"));
    }
}
