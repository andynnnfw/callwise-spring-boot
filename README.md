# Callwise

Queue management system with a real-time display panel. The attendant calls the next ticket and the public panel updates instantly, without reloading the page.

> 🚧 Work in progress

## Features

- [ ] Issue regular and priority tickets
- [ ] Priority queue (2 regular, 1 priority)
- [ ] Attendant screen: call, recall, finish, mark as absent
- [ ] Real-time public panel with sound alert
- [ ] Attendant login
- [ ] Online deployment

## Tech stack

Java 21 · Spring Boot · Spring WebSocket · Spring Data JPA · PostgreSQL · Docker · HTML, CSS and JavaScript

## How to run

Requirements: Java 21 and Docker.

1. Copy the environment template and set your own database password:
   ```bash
   cp .env.example .env
   ```
2. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run        # Linux / macOS
   .\mvnw.cmd spring-boot:run    # Windows
   ```
4. The app runs at `http://localhost:8080`.

## Screenshots

_Coming soon._

## Author

Anderson Melo do Nascimento
