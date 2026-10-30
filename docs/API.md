# API reference

Callwise exposes a small REST API, used by the kiosk and the attendant screen, and a WebSocket topic, used by the public panel.

- [Base URL](#base-url)
- [Authentication](#authentication)
- [Ticket object](#ticket-object)
- [Error format](#error-format)
- [Endpoints](#endpoints)
  - [Issue a ticket](#issue-a-ticket)
  - [Call the next ticket](#call-the-next-ticket)
  - [Recall a ticket](#recall-a-ticket)
  - [Finish a ticket](#finish-a-ticket)
  - [Mark a ticket as absent](#mark-a-ticket-as-absent)
- [Real-time panel (WebSocket)](#real-time-panel-websocket)
- [Trying it with curl](#trying-it-with-curl)

## Base URL

| Environment | URL |
|---|---|
| Local | `http://localhost:8080` |
| Online demo | `https://callwise-4hun.onrender.com` |

All request and response bodies are JSON (`Content-Type: application/json`).

## Authentication

| Endpoint | Access |
|---|---|
| `POST /api/senhas` | Public |
| `POST /api/fila/**` | Attendant session required |

The attendant API uses the same **session cookie** as the attendant screen:

1. Send the login form to `POST /login` with the fields `username` and `password` (`application/x-www-form-urlencoded`).
2. On success, the server answers `302` to `/atendente/index.html` and sets the `JSESSIONID` cookie.
3. Send that cookie in the next requests.

A request to `/api/fila/**` without a valid session is **redirected (`302`) to `/login/index.html`**. It does not return `401`.

Logout is `POST /logout`.

## Ticket object

Every endpoint that returns a ticket uses the same format (`SenhaResponse`):

```json
{
  "id": 7,
  "codigo": "N003",
  "tipo": "NORMAL",
  "status": "CHAMADA",
  "emitidaEm": "2026-10-30T09:15:42.123456"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | number | Ticket id. Used in the attendant endpoints |
| `codigo` | string | Code shown to people: `N` (regular) or `P` (priority) + three digits |
| `tipo` | string | `NORMAL` or `PRIORITARIA` |
| `status` | string | `AGUARDANDO` (waiting), `CHAMADA` (called), `ATENDIDA` (served) or `AUSENTE` (absent) |
| `emitidaEm` | string | Date and time the ticket was issued (ISO-8601, Brasília time, no offset) |

## Error format

Every API error has the same body (`ErroResponse`), produced by `GlobalExceptionHandler`:

```json
{
  "status": 409,
  "mensagem": "Esta senha não está em atendimento",
  "caminho": "/api/fila/7/finalizar"
}
```

| Field | Description |
|---|---|
| `status` | HTTP status code, repeated in the body |
| `mensagem` | Human-readable message in Portuguese, safe to show on screen |
| `caminho` | Path of the request that failed |

Errors the API can return:

| Status | When | `mensagem` |
|---|---|---|
| 400 | `tipo` is missing in the body | `Informe o tipo da senha: NORMAL ou PRIORITARIA` |
| 400 | Body is not valid JSON, or `tipo` is not a known type (for example `"VIP"`) | `Corpo da requisição inválido. Confira o JSON e se o tipo é NORMAL ou PRIORITARIA` |
| 400 | Ticket id in the path is not a number | `Valor inválido para id: abc` |
| 404 | No ticket with this id | `Senha não encontrada` |
| 409 | The ticket is not being served (status is not `CHAMADA`) | `Esta senha não está em atendimento` |
| 500 | Any unexpected error. Details go only to the server log | `Erro interno inesperado. Tente novamente mais tarde.` |

## Endpoints

### Issue a ticket

`POST /api/senhas` · public

Creates a ticket of the chosen type with the next number of the day. Used by the kiosk.

**Body**

```json
{ "tipo": "PRIORITARIA" }
```

| Field | Required | Values |
|---|---|---|
| `tipo` | yes | `NORMAL` or `PRIORITARIA` |

**Responses**

| Status | Body |
|---|---|
| `201 Created` | The new ticket, with `status` `AGUARDANDO` |
| `400 Bad Request` | Error (missing or invalid `tipo`, invalid JSON) |

```json
{
  "id": 12,
  "codigo": "P004",
  "tipo": "PRIORITARIA",
  "status": "AGUARDANDO",
  "emitidaEm": "2026-10-30T10:02:11.481203"
}
```

Numbering is separate for each type and restarts every day: the first regular ticket of the day is always `N001` and the first priority ticket is `P001`.

### Call the next ticket

`POST /api/fila/chamar` · attendant

Picks the next waiting ticket of the day using the [2 regular : 1 priority rule](ARCHITECTURE.md#queue-rule), changes its status to `CHAMADA` and **announces it on the public panel**.

**Responses**

| Status | Body |
|---|---|
| `200 OK` | The called ticket, with `status` `CHAMADA` |
| `204 No Content` | Empty. There is no waiting ticket today |

### Recall a ticket

`POST /api/fila/{id}/rechamar` · attendant

Calls the current ticket again: updates the call time and **announces it on the public panel again**. The status stays `CHAMADA`.

**Responses**

| Status | Body |
|---|---|
| `200 OK` | The recalled ticket |
| `400 Bad Request` | `id` is not a number |
| `404 Not Found` | No ticket with this id |
| `409 Conflict` | The ticket is not in `CHAMADA` |

### Finish a ticket

`POST /api/fila/{id}/finalizar` · attendant

Ends the service. The status changes to `ATENDIDA`. Nothing is sent to the panel.

**Responses**

| Status | Body |
|---|---|
| `200 OK` | The finished ticket, with `status` `ATENDIDA` |
| `400 Bad Request` | `id` is not a number |
| `404 Not Found` | No ticket with this id |
| `409 Conflict` | The ticket is not in `CHAMADA` (for example, it was already finished) |

### Mark a ticket as absent

`POST /api/fila/{id}/ausente` · attendant

Used when the customer does not show up. The status changes to `AUSENTE`. Nothing is sent to the panel.

**Responses**

| Status | Body |
|---|---|
| `200 OK` | The ticket, with `status` `AUSENTE` |
| `400 Bad Request` | `id` is not a number |
| `404 Not Found` | No ticket with this id |
| `409 Conflict` | The ticket is not in `CHAMADA` |

## Real-time panel (WebSocket)

| Item | Value |
|---|---|
| Endpoint | `/ws` (SockJS) |
| Protocol | STOMP |
| Topic | `/topic/painel` |
| Message | A [ticket object](#ticket-object) |
| Sent when | A ticket is called (`/chamar`) or recalled (`/rechamar`) |
| Access | Public |

Minimal browser client, the same libraries the panel uses:

```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.5.0/bundles/stomp.umd.min.js"></script>
<script>
  const client = new StompJs.Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe('/topic/painel', message => {
        const ticket = JSON.parse(message.body);
        console.log('Called:', ticket.codigo);
      });
    }
  });
  client.activate();
</script>
```

The endpoint uses SockJS, so plain WebSocket clients must connect to the SockJS transport URL (`/ws/{server}/{session}/websocket`) instead of `/ws`.

## Trying it with curl

```bash
BASE=http://localhost:8080

# Kiosk: issue a regular ticket (public)
curl -X POST $BASE/api/senhas \
  -H "Content-Type: application/json" \
  -d '{"tipo":"NORMAL"}'

# Attendant: log in and keep the session cookie
curl -c cookies.txt -X POST $BASE/login \
  -d username=atendente -d password=YOUR_PASSWORD

# Call the next ticket
curl -b cookies.txt -X POST $BASE/api/fila/chamar

# Finish ticket 1
curl -b cookies.txt -X POST $BASE/api/fila/1/finalizar
```

On the online demo the attendant password is `callwise-demo`.
