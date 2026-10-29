const MAX_CHAMADAS_ANTERIORES = 5;
const SEGUNDOS_PARA_RECONECTAR = 3;

const telaAtivar = document.getElementById('tela-ativar');
const btnAtivar = document.getElementById('btn-ativar');
const erroCarregar = document.getElementById('erro-carregar');
const painel = document.getElementById('painel');
const relogio = document.getElementById('relogio');
const chamada = document.getElementById('chamada');
const codigo = document.getElementById('codigo');
const tipo = document.getElementById('tipo');
const listaHistorico = document.getElementById('lista-historico');
const historicoVazio = document.getElementById('historico-vazio');
const conexao = document.getElementById('conexao');

const NOMES_DOS_TIPOS = {
    NORMAL: 'Normal',
    PRIORITARIA: 'Prioritária'
};

let som = null;
let senhaAtual = null;
let historico = [];

function formatarHora(data) {
    return data.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function atualizarRelogio() {
    relogio.textContent = formatarHora(new Date());
}

// ---------- Som ----------

// Aviso de duas notas (ding-dong) gerado pelo próprio navegador, sem arquivo de áudio.
function tocarAviso() {
    if (!som) {
        return;
    }
    som.resume();
    const agora = som.currentTime;
    tocarNota(659.25, agora);        // mi
    tocarNota(523.25, agora + 0.35); // dó
}

function tocarNota(frequencia, inicio) {
    const oscilador = som.createOscillator();
    const volume = som.createGain();

    oscilador.type = 'sine';
    oscilador.frequency.value = frequencia;

    volume.gain.setValueAtTime(0.0001, inicio);
    volume.gain.exponentialRampToValueAtTime(0.4, inicio + 0.02);
    volume.gain.exponentialRampToValueAtTime(0.0001, inicio + 1.4);

    oscilador.connect(volume).connect(som.destination);
    oscilador.start(inicio);
    oscilador.stop(inicio + 1.5);
}

// ---------- Tela ----------

function mostrarChamada(senha) {
    codigo.textContent = senha.codigo;
    tipo.textContent = NOMES_DOS_TIPOS[senha.tipo] || senha.tipo;

    // Trocar a classe e forçar um reflow reinicia a animação,
    // inclusive quando a mesma senha é chamada de novo.
    chamada.className = 'chamada ' + senha.tipo.toLowerCase();
    void chamada.offsetWidth;
    chamada.classList.add('chegou');
}

function criarTexto(classe, texto) {
    const elemento = document.createElement('span');
    elemento.className = classe;
    elemento.textContent = texto;
    return elemento;
}

function criarItemHistorico(item) {
    const li = document.createElement('li');
    li.className = 'historico-item ' + item.senha.tipo.toLowerCase();
    li.append(
        criarTexto('historico-codigo', item.senha.codigo),
        criarTexto('historico-tipo', NOMES_DOS_TIPOS[item.senha.tipo] || item.senha.tipo),
        criarTexto('historico-hora', formatarHora(item.hora))
    );
    return li;
}

function mostrarHistorico() {
    listaHistorico.replaceChildren(...historico.map(criarItemHistorico));
    historicoVazio.hidden = historico.length > 0;
}

function receberChamada(senha) {
    // A senha que estava na tela desce para o histórico.
    // Se for a mesma senha sendo rechamada, ela só pisca de novo.
    if (senhaAtual && senhaAtual.senha.id !== senha.id) {
        historico.unshift(senhaAtual);
    }

    // Uma senha antiga rechamada sai do histórico e volta para o destaque.
    historico = historico
        .filter(item => item.senha.id !== senha.id)
        .slice(0, MAX_CHAMADAS_ANTERIORES);

    senhaAtual = { senha: senha, hora: new Date() };

    mostrarChamada(senha);
    mostrarHistorico();
    tocarAviso();
}

// ---------- Conexão (WebSocket com STOMP) ----------

function mostrarConexao(estado, texto) {
    conexao.className = 'conexao ' + estado;
    conexao.textContent = texto;
}

function avisarSemConexao() {
    mostrarConexao('desconectado', 'Sem conexão com o servidor. Tentando de novo…');
}

function conectar() {
    const cliente = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: SEGUNDOS_PARA_RECONECTAR * 1000,
        onConnect: () => {
            mostrarConexao('conectado', 'Conectado');
            cliente.subscribe('/topic/painel', mensagem => receberChamada(JSON.parse(mensagem.body)));
        },
        onWebSocketClose: avisarSemConexao,
        onStompError: avisarSemConexao
    });

    cliente.activate();
}

// ---------- Ativação ----------

function ativarPainel() {
    if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') {
        erroCarregar.textContent = 'Não foi possível carregar o painel. Verifique a internet deste aparelho e recarregue a página.';
        erroCarregar.hidden = false;
        return;
    }

    // O navegador só libera o som dentro de um clique.
    // O aviso toca uma vez agora para confirmar que o som está funcionando.
    const ContextoDeAudio = window.AudioContext || window.webkitAudioContext;
    if (ContextoDeAudio) {
        som = new ContextoDeAudio();
        tocarAviso();
    }

    telaAtivar.hidden = true;
    painel.hidden = false;

    atualizarRelogio();
    setInterval(atualizarRelogio, 1000);

    conectar();
}

btnAtivar.addEventListener('click', ativarPainel);
