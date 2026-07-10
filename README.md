# ♟ Chess Arena

A **real-time multiplayer chess platform** built with Spring Boot. Players log in, find an
opponent from the online lobby, play a live game over WebSockets with every move validated
server-side by a hand-written chess engine, and accumulate a score that feeds a global
leaderboard.

Built to showcase clean **object-oriented design** (all four pillars + SOLID) with a
strong, server-authoritative backend.

---

## Features

- **Auth** — register / login with JWT; BCrypt-hashed passwords; the same token secures
  REST calls and the WebSocket handshake.
- **Lobby & matchmaking** — click **Find Opponent** → your challenge is broadcast to every
  online player → the **first to accept** is matched (race-safe). Colors are coin-flipped.
- **Live play** — STOMP over WebSocket; the board updates in real time for both players.
- **A real chess engine** — pure Java, no libraries: legal moves, check, checkmate,
  stalemate, castling, en passant, promotion, draw detection. Unit-tested.
- **Server-authoritative** — illegal moves and out-of-turn moves are rejected by the
  server; clients can't cheat.
- **Scores & history** — win = +3, draw = +1, loss = 0; accumulated per user and persisted,
  with a leaderboard and per-player game history.
- **Resilience** — disconnect mid-game forfeits to the opponent; scores settle immediately.

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, WebSocket, Security, Data JPA, Validation) |
| Real-time | STOMP over WebSocket (+ SockJS fallback) |
| Auth | Spring Security + JWT (jjwt) |
| Database | H2 (file mode) — swappable for MySQL/PostgreSQL |
| Frontend | Plain HTML / CSS / vanilla JS (no build step) |
## Quick start

```bash
cd chess-arena
./mvnw spring-boot:run       # needs only JDK 21+
# open http://localhost:8080  (use two browser windows to play yourself)
```

Full walkthrough: [docs/10-setup-and-run.md](docs/10-setup-and-run.md).

## How it works (in one diagram)

```
Browser (JS)  ──REST /api/auth──►  AuthController ─► UserService ─► H2
     │                                                     ▲
     │  STOMP /ws  (JWT on CONNECT)                        │ scores
     ▼                                                     │
LobbyController ─► MatchmakingService ─► GameService ─► ChessGame ENGINE
     │  broadcast challenges              │ validate moves   (pure Java)
     ▼                                    ▼
 /topic/lobby                       /topic/game/{id}  ──► both players
```

## Project layout

```
chess-arena/
├── src/main/java/com/chessarena/
│   ├── engine/        pure-Java chess engine (core / pieces / game)
│   ├── auth/          JWT auth (service, filter, controller)
│   ├── user/          User entity, repository, service, REST
│   ├── presence/      PresenceStore (in-memory )
│   ├── matchmaking/   Challenge + MatchmakingService
│   ├── game/          GameSession, GameService, GameRecord
│   ├── ws/            STOMP controllers, broadcasters, messages
│   └── config/        SecurityConfig, WebSocketConfig
├── src/main/resources/static/   index.html, css, js (frontend)
└── docs/              full design documentation (see below)
```

## Documentation

Design docs were written alongside the code and live in [`docs/`](docs/):

| Doc | Topic |
|---|---|
| [00-architecture.md](docs/00-architecture.md) | System overview & decisions |
| [01-project-setup.md](docs/01-project-setup.md) | How it was bootstrapped, config |
| [02-chess-engine.md](docs/02-chess-engine.md) | Engine design & rule coverage |
| [03-authentication.md](docs/03-authentication.md) | JWT & security |
| [04-persistence.md](docs/04-persistence.md) | Entities, repositories, scoring |
| [06-websocket-protocol.md](docs/06-websocket-protocol.md) | Every STOMP destination & message |
| [07-matchmaking-and-game-flow.md](docs/07-matchmaking-and-game-flow.md) | Challenge → accept → game lifecycle |
| [08-frontend.md](docs/08-frontend.md) | UI structure & board interaction |
| [09-oop-principles.md](docs/09-oop-principles.md) | **Principle-by-principle mapping to code** |
| [10-setup-and-run.md](docs/10-setup-and-run.md) | Run, test, and play |

## Tests

```bash
./mvnw test -Dtest=ChessEngineTest   # 5 engine tests: checkmate, castling, promotion, …
```

The complete multiplayer flow (register → matchmaking → live moves → checkmate → score
accumulation) was verified end-to-end against the live server. See
[docs/10-setup-and-run.md](docs/10-setup-and-run.md#headless-end-to-end-check).

## Status

All planned features are implemented and verified working:
auth ✓ · lobby/presence ✓ · find-opponent + accept ✓ · live server-validated play ✓ ·
checkmate/stalemate/draw/resign/disconnect ✓ · score accumulation + leaderboard ✓.
