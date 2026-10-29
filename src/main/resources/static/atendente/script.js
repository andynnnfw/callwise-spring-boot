const CHAVE_SENHA_ATUAL = 'callwise.atendente.senhaAtual';

const telaLivre = document.getElementById('livre');
const telaOcupado = document.getElementById('ocupado');
const btnChamar = document.getElementById('btn-chamar');
const btnRechamar = document.getElementById('btn-rechamar');
const btnAusente = document.getElementById('btn-ausente');
const btnFinalizar = document.getElementById('btn-finalizar');
const codigo = document.getElementById('codigo');
const tipo = document.getElementById('tipo');
const retirada = document.getElementById('retirada');
const aviso = document.getElementById('aviso');

const BOTOES = [btnChamar, btnRechamar, btnAusente, btnFinalizar];

const NOMES_DOS_TIPOS = {
    NORMAL: 'Normal',
    PRIORITARIA: 'Prioritária'
};

let senhaAtual = lerSenhaSalva();

// ---------- Senha em atendimento, salva no navegador ----------

// Se a página for recarregada no meio de um atendimento, a senha continua na tela.
function lerSenhaSalva() {
    try {
        const salva = localStorage.getItem(CHAVE_SENHA_ATUAL);
        return salva ? JSON.parse(salva) : null;
    } catch (e) {
        return null;
    }
}

function guardarSenha(senha) {
    senhaAtual = senha;
    try {
        if (senha) {
            localStorage.setItem(CHAVE_SENHA_ATUAL, JSON.stringify(senha));
        } else {
            localStorage.removeItem(CHAVE_SENHA_ATUAL);
        }
    } catch (e) {
        // Sem armazenamento a tela funciona igual; só não lembra a senha depois de recarregar.
    }
}

// ---------- Tela ----------

function formatarHora(dataHora) {
    return new Date(dataHora).toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function mostrarTela() {
    const atendendo = senhaAtual !== null;
    telaLivre.hidden = atendendo;
    telaOcupado.hidden = !atendendo;

    if (atendendo) {
        codigo.textContent = senhaAtual.codigo;
        tipo.textContent = NOMES_DOS_TIPOS[senhaAtual.tipo] || senhaAtual.tipo;
        tipo.className = 'senha-tipo ' + senhaAtual.tipo.toLowerCase();
        retirada.textContent = 'Retirada às ' + formatarHora(senhaAtual.emitidaEm);
    }
}

function avisar(texto, ehErro) {
    aviso.textContent = texto;
    aviso.classList.toggle('erro', Boolean(ehErro));
}

function bloquearBotoes(bloquear) {
    BOTOES.forEach(botao => {
        botao.disabled = bloquear;
    });
}

// ---------- Ações ----------

// Bloqueia os botões durante a requisição para evitar clique duplo.
// Cada ação devolve o botão que deve ficar em foco depois: o próximo passo provável.
// (Um botão desabilitado não recebe foco, por isso o foco só vem depois de liberar.)
async function executar(acao) {
    bloquearBotoes(true);
    let proximoFoco = null;
    try {
        proximoFoco = await acao();
    } catch (e) {
        avisar('Não foi possível falar com o servidor. Tente de novo.', true);
    } finally {
        bloquearBotoes(false);
    }
    if (proximoFoco) {
        proximoFoco.focus();
    }
}

// Faz o POST na API da fila. Se a sessão de login terminou, o servidor responde 401
// (ou redireciona para o login) e a tela vai direto para a página de login.
async function postarNaFila(caminho) {
    const resposta = await fetch('/api/fila' + caminho, { method: 'POST' });
    if (resposta.status === 401 || resposta.redirected) {
        window.location.href = '/login/index.html?expirou';
        // A página vai mudar: a promessa nunca termina, então a ação para aqui
        // e os botões ficam bloqueados até a troca de página.
        return new Promise(() => {});
    }
    return resposta;
}

async function chamarProxima() {
    const resposta = await postarNaFila('/chamar');

    if (resposta.status === 204) {
        avisar('Ninguém aguardando na fila agora.');
        return btnChamar;
    }
    if (!resposta.ok) {
        throw new Error('Resposta ' + resposta.status);
    }

    guardarSenha(await resposta.json());
    mostrarTela();
    avisar(senhaAtual.codigo + ' chamada no painel.');
    return btnFinalizar;
}

// Rechamar, marcar ausente e finalizar agem sobre a senha em atendimento.
async function agirNaSenhaAtual(acao, encerraAtendimento, mensagem) {
    const senha = senhaAtual;
    const resposta = await postarNaFila('/' + senha.id + '/' + acao);

    // 404: a senha não existe mais. 409: ela já não está chamada (foi encerrada em outro lugar).
    // Nos dois casos a tela volta a ficar livre para chamar a próxima.
    if (resposta.status === 404 || resposta.status === 409) {
        guardarSenha(null);
        mostrarTela();
        avisar(resposta.status === 404
            ? 'A senha ' + senha.codigo + ' não existe mais. Chame a próxima.'
            : 'A senha ' + senha.codigo + ' já tinha sido encerrada. Chame a próxima.', true);
        return btnChamar;
    }
    if (!resposta.ok) {
        throw new Error('Resposta ' + resposta.status);
    }

    avisar(mensagem(senha.codigo));
    if (!encerraAtendimento) {
        return btnRechamar;
    }
    guardarSenha(null);
    mostrarTela();
    return btnChamar;
}

btnChamar.addEventListener('click', () => executar(chamarProxima));

btnRechamar.addEventListener('click', () => executar(() =>
    agirNaSenhaAtual('rechamar', false, c => c + ' rechamada no painel.')));

btnAusente.addEventListener('click', () => executar(() =>
    agirNaSenhaAtual('ausente', true, c => c + ' marcada como ausente.')));

btnFinalizar.addEventListener('click', () => executar(() =>
    agirNaSenhaAtual('finalizar', true, c => 'Atendimento da ' + c + ' finalizado.')));

mostrarTela();
