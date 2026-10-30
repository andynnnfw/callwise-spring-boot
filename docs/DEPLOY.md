# Deploy

Callwise runs online as a Docker container on **Render**, with the database on **Neon** (managed PostgreSQL). Both are on free plans.

**Live demo:** https://callwise-4hun.onrender.com

- [How it is deployed](#how-it-is-deployed)
- [Docker image](#docker-image)
- [Running the image locally](#running-the-image-locally)
- [Environment variables](#environment-variables)
- [Database on Neon](#database-on-neon)
- [Web service on Render](#web-service-on-render)
- [Continuous deployment](#continuous-deployment)
- [Free plan limits](#free-plan-limits)
- [Troubleshooting](#troubleshooting)

## How it is deployed

```mermaid
flowchart LR
    Dev[git merge to main] --> GH[GitHub]
    GH -- auto-deploy --> R[Render<br/>builds the Dockerfile<br/>and runs the container]
    R -- JDBC over SSL --> N[(Neon<br/>PostgreSQL 16)]
    U[Visitors] -- HTTPS / WebSocket --> R
```

| Piece | Service | Plan | Region |
|---|---|---|---|
| Application | Render Web Service (Docker) | Free (0.1 CPU, 512 MB RAM) | Virginia (US East) |
| Database | Neon | Free (1 GB) | AWS US East 1 (N. Virginia) |

The application and the database are in the same region, so each query stays inside the same area of the network.

## Docker image

The `Dockerfile` has two stages:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
```

**Build stage** (JDK 21)

- Copies only the Maven Wrapper and `pom.xml` first and downloads the dependencies. Docker caches this layer, so later builds skip the download while `pom.xml` does not change.
- Then copies `src` and packages the jar.
- Tests are skipped (`-DskipTests`) because `CallwiseApplicationTests` needs a database, and there is none during the image build. Tests run before every pull request instead.

**Runtime stage** (JRE 21)

- Starts from a clean, smaller image and receives only the jar. Maven, the source code and the build cache stay behind.
- `-XX:MaxRAMPercentage=75` limits the Java heap to 75% of the container memory, which keeps the JVM inside the 512 MB of the free plan.
- `-Duser.timezone=America/Sao_Paulo` sets the JVM time zone. Containers run in UTC by default, and the app uses `LocalDate.now()` to know which tickets belong to "today". Without this flag, from 9 p.m. Brasília time the system would think it was already the next day: numbering would restart at `001` and the tickets of the evening would disappear from the queue.

**`.dockerignore`** keeps `target`, `.git`, `.idea`, `docs` and, most importantly, `.env` out of the build context, so local passwords can never end up inside an image.

## Running the image locally

With the local database running (`docker compose up -d`):

```bash
docker build -t callwise .

docker run --rm -p 8080:8080 --env-file .env \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/callwise \
  callwise
```

- `--env-file .env` passes the database user and password and the attendant login.
- `DB_URL` must point to `host.docker.internal`, because inside the container `localhost` is the container itself, not your computer.
- To simulate Render, add `-e PORT=10000` and map `-p 8080:10000`.

Environment variables are read only when the application starts. After changing `.env`, start the container again.

## Environment variables

Variables set on Render:

| Variable | Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://<neon-host>/callwise?sslmode=require` |
| `DB_USER` | Neon role (for example `callwise_owner`) |
| `DB_PASSWORD` | Neon role password |
| `ATENDENTE_USUARIO` | `atendente` |
| `ATENDENTE_SENHA` | `callwise-demo` (the demo account shown on the login page) |

`PORT` is not set by hand: Render defines it, and `server.port=${PORT:8080}` picks it up.

## Database on Neon

1. Create a project with **Postgres 16** (the same version as `docker-compose.yml`) in **AWS US East 1 (N. Virginia)**, with a database named `callwise`.
2. Open **Connect** and **turn off Connection pooling**. The application already has its own pool (HikariCP), so it uses the direct connection.
3. Copy the connection string. It looks like this:

   ```
   postgresql://callwise_owner:PASSWORD@ep-xxxx.us-east-1.aws.neon.tech/callwise?sslmode=require&channel_binding=require
   ```

4. Convert it to the three variables the application expects:

   | Variable | Part of the string |
   |---|---|
   | `DB_USER` | between `//` and `:` |
   | `DB_PASSWORD` | between `:` and `@` |
   | `DB_URL` | `jdbc:postgresql://` + everything after `@`, ending with `?sslmode=require` |

   The Java driver does not accept the `postgresql://user:password@...` format, and `channel_binding` has a different name in it, so that parameter is removed. `sslmode=require` keeps the connection encrypted.

The tables are created by Hibernate on the first start (`ddl-auto=update`).

## Web service on Render

1. **New → Web Service** and select the GitHub repository.
2. Settings:

   | Field | Value |
   |---|---|
   | Language | Docker |
   | Branch | `main` |
   | Region | Virginia (US East) |
   | Root Directory | empty |
   | Instance type | Free |

3. Under **Environment Variables**, use **Add from .env** and paste the variables from the table above.
4. **Deploy web service**. The first build takes a few minutes; the deploy is ready when the log shows `Started CallwiseApplication` and Render prints `Your service is live`.

Render supports WebSockets on web services, so the panel works without extra configuration.

## Continuous deployment

Render watches the `main` branch. Every merged pull request triggers a new build and deploy automatically. A deploy can also be started by hand with **Manual Deploy → Deploy latest commit**.

On the free plan there is no zero-downtime deploy, so the demo is unavailable for a short time while the new version starts.

Changing an environment variable in the Render dashboard also restarts the service with the new value.

## Free plan limits

| Limit | Effect |
|---|---|
| Render spins the service down after 15 minutes without requests | The next visit wakes it up. With 0.1 CPU, Spring Boot takes about 2 minutes to start, so the first page can be slow. After that, it is fast |
| Render free instance has 512 MB RAM and 0.1 CPU | Enough for a demo. The heap is limited to 75% of the memory |
| Neon free plan has 1 GB of storage and 100 compute hours per month | Far above what the demo uses. The database also suspends itself when idle, and if a limit were reached it would pause until the next month, with no charge |

Neon was chosen instead of Render's own PostgreSQL because Render's free database expires after 30 days, which would take the demo offline. See [DECISIONS.md](DECISIONS.md#hosting-render--neon).

## Troubleshooting

Problems you may hit when deploying this project. The first one and the login one actually happened during the first deploy; the others are prevented by the current configuration.

| Symptom | Cause | Fix |
|---|---|---|
| Build fails with `./mvnw: Permission denied` (exit code 126) | `mvnw` was stored in Git without the executable bit (`100644`). Docker Desktop on Windows marks every file as executable, so the local build worked, but Render builds from the Git checkout | `git update-index --chmod=+x mvnw` and commit. The file is now `100755` |
| Deploy hangs with "no open ports detected" | The app listened on 8080 while Render expects the port in `PORT` | `server.port=${PORT:8080}` |
| `Connection to localhost:5433 refused` in the container | The database URL pointed to `localhost` | `spring.datasource.url=${DB_URL:...}`, with `DB_URL` set on Render |
| `password authentication failed` | Wrong `DB_USER` or `DB_PASSWORD` | Copy them again from the Neon connection string |
| Attendant login rejects the password from `.env` | The container was started before `.env` changed | Restart the container (locally) or change the variable on Render |
| Ticket numbering restarts in the evening | JVM running in UTC | `-Duser.timezone=America/Sao_Paulo` in the `ENTRYPOINT` |
