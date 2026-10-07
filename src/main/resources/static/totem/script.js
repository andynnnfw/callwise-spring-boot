const SEGUNDOS_PARA_VOLTAR = 10;

const telaEscolha = document.getElementById('tela-escolha');
const telaSenha = document.getElementById('tela-senha');
const btnNormal = document.getElementById('btn-normal');
const btnPrioritaria = document.getElementById('btn-prioritaria');
const btnConcluir = document.getElementById('btn-concluir');
const codigo = document.getElementById('codigo');
const tipo = document.getElementById('tipo');
const horario = document.getElementById('horario');
const contagem = document.getElementById('contagem');
const erro = document.getElementById('erro');

const NOMES_DOS_TIPOS = {
    NORMAL: 'Normal',
    PRIORITARIA: 'Prioritária'
};

let temporizador = null;

function bloquearBotoes(bloquear) {
    btnNormal.disabled = bloquear;
    btnPrioritaria.disabled = bloquear;
}

function mostrarSenha(senha) {
    codigo.textContent = senha.codigo;
    tipo.textContent = NOMES_DOS_TIPOS[senha.tipo] || senha.tipo;
    tipo.className = 'bilhete-tipo ' + senha.tipo.toLowerCase();
    horario.textContent = new Date(senha.emitidaEm).toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit'
    });

    telaEscolha.hidden = true;
    telaSenha.hidden = false;
    btnConcluir.focus();
    iniciarContagem();
}

function voltarParaEscolha() {
    clearInterval(temporizador);
    telaSenha.hidden = true;
    telaEscolha.hidden = false;
}

// O totem volta sozinho para a tela inicial, pronto para a próxima pessoa.
function iniciarContagem() {
    let restante = SEGUNDOS_PARA_VOLTAR;
    contagem.textContent = 'Voltando ao início em ' + restante + ' s';

    clearInterval(temporizador);
    temporizador = setInterval(() => {
        restante--;
        if (restante <= 0) {
            voltarParaEscolha();
            return;
        }
        contagem.textContent = 'Voltando ao início em ' + restante + ' s';
    }, 1000);
}

async function emitirSenha(tipoEscolhido) {
    bloquearBotoes(true);
    erro.hidden = true;

    try {
        const resposta = await fetch('/api/senhas', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tipo: tipoEscolhido })
        });

        if (!resposta.ok) {
            throw new Error('Resposta ' + resposta.status);
        }

        mostrarSenha(await resposta.json());
    } catch (e) {
        erro.textContent = 'Não foi possível emitir a senha. Toque de novo ou procure um atendente.';
        erro.hidden = false;
    } finally {
        bloquearBotoes(false);
    }
}

btnNormal.addEventListener('click', () => emitirSenha('NORMAL'));
btnPrioritaria.addEventListener('click', () => emitirSenha('PRIORITARIA'));
btnConcluir.addEventListener('click', voltarParaEscolha);
