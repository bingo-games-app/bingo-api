# Bingo

A real-time web app for hosting bingo rooms. A host creates a game, players join with a room code (as a guest or with an account), receive up to three cards, and the server validates who gets BINGO first.

> **Status:** MVP in development. Target: first playable version before December 2026.

This repository contains the **backend API**. The frontend lives in [`bingo-web`](https://github.com/bingo-games-app/bingo-web).

## MVP rules

- **75-ball bingo** (B-I-N-G-O columns: B 1-15, I 16-30, N 31-45, G 46-60, O 61-75).
- **Full card** is the only winning pattern in the MVP. Each card is 5x5 with a free center space (24 numbers).
- **Server-side validation:** the server decides whether a BINGO claim is valid. The first valid claim wins and ends the game.
- **Up to 3 cards per player** per game. Cards are unique within a game.
- **Two draw modes**, chosen by the host when creating the game and **immutable afterwards**:
  - `AUTOMATIC`: the app shuffles 1-75 when the game starts and the host presses a button to reveal the next number.
  - `MANUAL`: the host draws physical balls and only registers which numbers were called.
- **Players:** guests (nickname + server-issued token) or registered users. Hosts need an account (email + password).
- **Players mark their own numbers.** A number can only be marked after it has been called, and a player can miss a call if they do not mark it.
- **Big-screen view:** a read-only page lets the host project the board and the called numbers on a TV or projector.
- **Real time:** called numbers are pushed to clients with Server-Sent Events (SSE).

Game lifecycle: `WAITING` → `RUNNING` → `FINISHED` (or `CANCELLED`).

## Tech stack

| Area                 | Technology                                   |
| -------------------- | -------------------------------------------- |
| Language / framework | Java 17+ (21 recommended), Spring Boot 3     |
| Data access          | Spring `JdbcClient` (plain JDBC, **no JPA**) |
| Database             | PostgreSQL                                   |
| Migrations           | Flyway                                       |
| Build                | Maven (wrapper included)                     |
| Real time            | Server-Sent Events                           |
| CI                   | GitHub Actions                               |
| Containers           | Docker, Docker Compose                       |
| Frontend             | React + TypeScript (see `bingo-web`)         |

## Getting started

### Prerequisites

- Java 17 or newer
- Docker and Docker Compose
- Git

### Run locally

```bash
# 1. Clone
git clone https://github.com/bingo-games-app/bingo-api.git
cd bingo-api

# 2. Create your local environment file
cp .env.example .env

# 3. Start PostgreSQL
docker compose up -d

# 4. Start the API (Flyway applies the migrations on startup)
./gradlew bootRun spring-boot:run
```

### Run the tests1

```bash
./gradlew test
```

## Configuration

Configuration is read from environment variables. Copy `.env.example` to `.env` and adjust it. Never commit `.env`.

| Variable                     | Description                                             |
| ---------------------------- | ------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`      | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/bingo` |
| `SPRING_DATASOURCE_USERNAME` | Database user                                           |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                       |

## Project structure

```text
.
├── .github/                      # PR template and CI workflows
├── docs/                         # Database diagram (bingo.dbml), ADRs, API contract
├── src/
│   ├── main/
│   │   ├── java/                 # Application code
│   │   └── resources/
│   │       └── db/migration/     # Flyway migrations (V1__init_schema.sql, ...)
│   └── test/                     # Tests
├── docker-compose.yml            # Local PostgreSQL
├── .env.example                  # Environment variable template
├── CONTRIBUTING.md
└── README.md
```

## Database

The schema is defined by Flyway migrations in `src/main/resources/db/migration/`, and documented in [`docs/bingo.dbml`](docs/bingo.dbml). Main tables: `app_user`, `game`, `participant`, `card`, `card_cell`, `drawn_number`.

Important rules enforced by the database:

- `game.mode` cannot be changed after creation (trigger).
- A ball cannot be drawn twice in the same game (`UNIQUE (game_id, number)`).
- A participant has at most 3 cards, and a card number must fit its column range.

**Never edit a migration that has already been applied.** Add a new `V<next>__description.sql` instead.

## Roadmap

Planned after the MVP:

- Phone-number login with one-time code (OTP) and persistent accounts
- Resume a room if the host closes the browser
- Native Docker image (GraalVM)
- More winning patterns and 90-ball bingo
- Multiple winners on ties

## Team

- Guilherme Feitosa ([@GuilhermeFeitoza08](https://github.com/GuilhermeFeitoza08))
- Yuri Nunes ([@YuriDNunes](https://github.com/YuriDNunes))
- Calil Rodrigues ([@Calil144](https://github.com/Calil144))
