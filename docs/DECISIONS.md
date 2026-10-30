# Technical decisions

The main choices made while building Callwise, why they were made and what they cost. The last section lists the known limitations of the current version.

- [Single application for API and screens](#single-application-for-api-and-screens)
- [Plain HTML, CSS and JavaScript](#plain-html-css-and-javascript)
- [Queue rule as a pure component](#queue-rule-as-a-pure-component)
- [Daily numbering protected by a unique constraint](#daily-numbering-protected-by-a-unique-constraint)
- [WebSocket with STOMP and SockJS](#websocket-with-stomp-and-sockjs)
- [Announcing on the panel after the transaction](#announcing-on-the-panel-after-the-transaction)
- [DTOs instead of entities in the API](#dtos-instead-of-entities-in-the-api)
- [One error format for the whole API](#one-error-format-for-the-whole-api)
- [Form login with a single attendant](#form-login-with-a-single-attendant)
- [CSRF disabled](#csrf-disabled)
- [Configuration through environment variables](#configuration-through-environment-variables)
- [Brasília time everywhere](#brasília-time-everywhere)
- [Schema managed by Hibernate](#schema-managed-by-hibernate)
- [Multi-stage Docker image](#multi-stage-docker-image)
- [Hosting: Render + Neon](#hosting-render--neon)
- [Testing strategy](#testing-strategy)
- [Known limitations](#known-limitations)

## Single application for API and screens

**Decision:** Spring Boot serves the REST API, the WebSocket endpoint and the four screens from the same application.

**Why:** one deploy, one URL and no CORS configuration. The screens call the API with relative paths (`/api/...`, `/ws`), and the session cookie of the attendant works on both the page and the API.

**Trade-off:** the front-end cannot be deployed or scaled separately. For a system of this size, that is not a problem.

## Plain HTML, CSS and JavaScript

**Decision:** no front-end framework and no build step. Each screen is an `index.html`, a `style.css` and a `script.js`.

**Why:** the screens are small and each one has a single job. Plain JavaScript keeps the project easy to read, starts fast on modest devices (a kiosk or a TV) and keeps the focus of the project on the backend.

**Trade-off:** no components or shared state between screens. Some small helpers (for example, the names of the ticket types) are repeated in each script.

## Queue rule as a pure component

**Decision:** the "who is next" rule lives in `RegraFila`, a component with no database access that receives three facts (regular tickets called in a row, whether there are regular tickets waiting, whether there are priority tickets waiting) and returns the next type.

**Why:** the rule is the heart of the system and the part most likely to change. Isolated from JPA, it is covered by fast unit tests that describe every case by name (`chamaPrioritariaDepoisDeDuasNormais`, `chamaPrioritariaSeNaoTemNormal`...). `FilaService` only collects the facts and applies the answer.

**Rule chosen:** 2 regular : 1 priority. Priority tickets are served much sooner, but a steady flow of priority customers cannot block the regular queue forever. Details and an example are in [ARCHITECTURE.md](ARCHITECTURE.md#queue-rule).

## Daily numbering protected by a unique constraint

**Decision:** each type has its own sequence that restarts every day (`N001`, `P001`...). The next number is `max(numero) + 1` for the type and the day, and the table has a unique constraint on `(tipo, data_emissao, numero)`.

**Why:** short codes are easy to read on a TV and to say out loud, which is how real service queues work. The constraint is the last line of defense: even if two requests compute the same number at the same time, the database will never store a duplicated code.

**Trade-off:** see [Known limitations](#known-limitations) for what happens to the second request in that race.

## WebSocket with STOMP and SockJS

**Decision:** the panel receives calls through a WebSocket, using STOMP over SockJS and Spring's simple in-memory broker, on the topic `/topic/painel`.

**Why:**

- **Instead of polling:** the panel must show a call the moment it happens, and polling every few seconds would either be slow or flood the server with requests from every TV.
- **STOMP:** gives publish/subscribe with topics out of the box. The backend publishes with one line (`convertAndSend`), and any number of panels can subscribe.
- **SockJS:** falls back to other transports where raw WebSockets are blocked (some corporate networks and proxies).
- **Simple broker:** no external message broker to install or pay for.

**Trade-off:** the simple broker lives in the application memory, so it only works with **one** application instance. Running several instances would require an external broker (for example RabbitMQ) relaying STOMP messages.

## Announcing on the panel after the transaction

**Decision:** `FilaController` calls `PainelService.anunciar(...)` **after** `FilaService` returns, instead of sending the message inside the service.

**Why:** the service methods are `@Transactional`, and the transaction commits when they return. Announcing afterwards guarantees that the panel never shows a ticket whose change failed to save. It also keeps `FilaService` free of messaging code, so its tests only deal with the queue.

**Rule:** only calls and recalls are announced. Finishing a ticket or marking it as absent does not change what people in the waiting room need to see.

## DTOs instead of entities in the API

**Decision:** the API receives `EmitirSenhaRequest` and returns `SenhaResponse` (Java records). The `Senha` entity is never serialized directly.

**Why:** the API exposes only what the screens need, in a stable format. Internal fields (`numero`, `dataEmissao`, timestamps used by the queue) can change without breaking clients, and Jackson never touches JPA lazy-loading.

## One error format for the whole API

**Decision:** business errors are thrown as `ResponseStatusException` (404, 409) in the services, and a `@RestControllerAdvice` (`GlobalExceptionHandler`) converts every error into the same body: `{ status, mensagem, caminho }`.

**Why:** the screens show `mensagem` directly to the user, so every error needed a clear message in Portuguese instead of Spring's default error page or a stack trace. Covered cases: validation (`@NotNull`), invalid JSON or enum value, non-numeric path id, business errors and a fallback for anything unexpected.

**Security detail:** the fallback returns a generic message and logs the exception with its stack trace on the server. Messages of unexpected exceptions can contain SQL or class names, so they never reach the client.

**Scope:** the advice is limited to the `controller` package, so static files and Spring's own endpoints keep their default behaviour.

## Form login with a single attendant

**Decision:** Spring Security form login, with a custom login page and one user defined by environment variables (`ATENDENTE_USUARIO`, `ATENDENTE_SENHA`).

**Why:** the MVP has one service counter, so one account covers the real use case without a user table, registration or password reset. Form login with a session cookie works naturally with plain HTML pages, and the same session protects the attendant page and the attendant API.

**Demo account:** the online demo uses the public account `atendente` / `callwise-demo`, shown on the login page, so visitors can try the attendant screen.

**Next step:** multiple counters, each with its own attendant, is on the roadmap.

## CSRF disabled

**Decision:** CSRF protection is turned off in `SecurityConfig`.

**Why:** the attendant screen calls the API with `fetch` from pages of the same origin, and keeping CSRF on would require reading the token and sending it in every request. For the MVP, the simpler setup was chosen.

**Risk and mitigation:** with CSRF off, a malicious site could try to send a cross-site `POST` to `/api/fila/...` using the attendant's session. Chromium-based browsers (Chrome, Edge) treat cookies without a `SameSite` attribute as `SameSite=Lax`, which blocks the session cookie on cross-site `POST` requests, but other browsers do not, so this is only a partial mitigation. The impact is also limited: the worst case is a ticket being called, finished or marked absent. Turning CSRF back on, with the token sent by the attendant screen, is listed as an improvement.

## Configuration through environment variables

**Decision:** every value that changes between environments (`PORT`, `DB_URL`, database credentials, attendant login) comes from an environment variable, with defaults for local development. Locally the variables come from a `.env` file that is ignored by Git.

**Why:** the same jar and the same Docker image run on any machine and on Render without changes, and no password is ever committed. The `.env` file also feeds `docker-compose.yml`, so the database and the application use the same credentials.

## Brasília time everywhere

**Decision:** the JVM runs with `-Duser.timezone=America/Sao_Paulo` and Hibernate writes timestamps with `hibernate.jdbc.time_zone=America/Sao_Paulo`.

**Why:** "today" is a business concept in this system: numbering restarts and the queue only considers tickets issued today (`LocalDate.now()`). Servers and containers usually run in UTC, three hours ahead of Brasília, so without this setting the day would change at 9 p.m. local time and evening tickets would leave the queue.

## Schema managed by Hibernate

**Decision:** `spring.jpa.hibernate.ddl-auto=update`.

**Why:** with a single table that changed often during development, letting Hibernate create and update it kept the focus on the features.

**Trade-off:** `update` never removes or renames columns and keeps no history of schema changes. Moving to versioned migrations with Flyway is on the roadmap.

## Multi-stage Docker image

**Decision:** a two-stage `Dockerfile`: JDK 21 to build, JRE 21 to run.

**Why:** the final image carries only the JRE and the jar, without Maven, the sources or the dependency cache. Copying `pom.xml` before `src` lets Docker cache the dependency download. Details are in [DEPLOY.md](DEPLOY.md#docker-image).

## Hosting: Render + Neon

**Decision:** the application runs on Render (free web service, Docker) and the database on Neon (free PostgreSQL), both in the US East region.

**Why:**

- **Render:** builds straight from the `Dockerfile`, deploys automatically on every merge to `main`, supports WebSockets and has a free plan with no credit card.
- **Neon instead of Render's PostgreSQL:** Render's free database expires after 30 days, which would take the demo offline. Neon's free plan has no time limit.
- **Instead of Railway:** Railway, the other option considered, charges by usage after a trial credit. Render + Neon keep the demo free with no time limit.
- **Same region:** Render has no region in South America. US East is the closest to Brazil, and keeping the app and the database together avoids a cross-country round trip on every query.

**Trade-off:** the free web service sleeps after 15 minutes without requests and takes about 2 minutes to wake up. For a portfolio demo, that is acceptable.

## Testing strategy

**Decision:**

- **Unit tests with Mockito** for the queue rule and the services, without Spring or a database.
- **`@WebMvcTest` slices** for security rules and the error format, with the services mocked. They test the real HTTP behaviour (status codes, redirects, JSON body) quickly.
- **One context test** (`CallwiseApplicationTests`) that starts the whole application against the local PostgreSQL.

**Why:** the important behaviour (who is called next, which errors the API returns, who can access what) is covered by fast tests that do not depend on a database. Tests are written before or together with the code, and the whole suite runs before every pull request.

## Known limitations

These are known and accepted for the current version:

| Limitation | Impact | Possible solution |
|---|---|---|
| Two tickets of the same type issued at the exact same moment can compute the same number | The unique constraint rejects the second one, which receives a `500` and must try again. Very unlikely with one kiosk | Retry on constraint violation, or a database sequence per type and day |
| Two attendants calling at the same moment could receive the same ticket | Not possible today, because there is a single attendant | Pessimistic lock (`SELECT ... FOR UPDATE SKIP LOCKED`) when picking the next ticket |
| Tickets not called by midnight stay `AGUARDANDO` forever | They no longer appear in the queue, which is the expected behaviour, but they are never closed | A scheduled job that marks them as absent at the end of the day |
| In-memory STOMP broker | Works with only one application instance | External broker relay (RabbitMQ) |
| CSRF disabled | See [CSRF disabled](#csrf-disabled) | Enable CSRF and send the token from the attendant screen |
| Single attendant account | No individual history per attendant | Users table and multiple counters |
| A class-level validation error would not be mapped by the validation handler | It would fall into the generic `500`. There are no class-level validations today | Fall back to a generic message when there is no field error |
| Schema managed by `ddl-auto=update` | No migration history | Flyway |
| Free hosting | Slow first request after the service sleeps | Paid instance, or a service that does not sleep |
