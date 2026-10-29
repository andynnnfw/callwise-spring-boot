const aviso = document.getElementById('aviso');
const campoUsuario = document.getElementById('usuario');
const campoSenha = document.getElementById('senha');
const btnEntrar = document.getElementById('btn-entrar');
const btnDemo = document.getElementById('btn-demo');

// O Spring Security volta para esta página com um parâmetro na URL
// (configurado no SecurityConfig): ?erro, ?saiu ou ?expirou.
const AVISOS = {
    erro: { texto: 'Usuário ou senha incorretos. Confira e tente de novo.', ehErro: true },
    saiu: { texto: 'Você saiu da tela do atendente.', ehErro: false },
    expirou: { texto: 'Sua sessão terminou. Entre de novo para continuar.', ehErro: false }
};

function mostrarAvisoDaUrl() {
    const parametros = new URLSearchParams(window.location.search);
    const chave = Object.keys(AVISOS).find(nome => parametros.has(nome));
    if (!chave) {
        return;
    }
    aviso.textContent = AVISOS[chave].texto;
    aviso.classList.toggle('erro', AVISOS[chave].ehErro);
    aviso.hidden = false;
}

// Preenche os campos com a conta mostrada no quadro, para quem está conhecendo o projeto.
function preencherDemo() {
    campoUsuario.value = document.getElementById('demo-usuario').textContent;
    campoSenha.value = document.getElementById('demo-senha').textContent;
    btnEntrar.focus();
}

btnDemo.addEventListener('click', preencherDemo);

mostrarAvisoDaUrl();
