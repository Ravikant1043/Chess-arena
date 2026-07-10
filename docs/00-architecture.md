# Chess Arena — Architecture Overview

> **Status:** Living document. Written before implementation began; updated as the system evolves.

## 1. What we are building

A real-time **multiplayer chess platform**:

1. A player registers / logs in (JWT authentication).
2. They land in a **lobby** showing all currently online players.
3. Clicking **"Find Opponent"** broadcasts a challenge to every online player.
4. Any online player may **accept** the challenge — the first to accept is matched, everyone else is informed the challenge is gone.
5. A live game begins over **WebSocket**; every move is validated **server-side** by our own chess engine.
6. When the game ends (checkmate / stalemate / resignation / disconnect) the result is **persisted** and each player's **accumulated score** is updated.

## 2. Technology choices

| Concern | Choice | Why |
|---|---|---|
| Language / runtime | Java 21 | Modern LTS; records, switch patterns available |
| Framework | Spring Boot 3.5.x | Mature, batteries-included, first-class WebSocket + Security |
| Build | Maven (wrapper `./mvnw`) | No global install needed |
| Real-time transport | STOMP over WebSocket (SockJS fallback) | Pub/sub semantics map perfectly to lobby broadcasts + per-game channels |
| Authentication | Spring Security + JWT (jjwt 0.12) | Stateless; the same token authenticates REST calls and the WebSocket handshake |
| Relational storage | H2 (file mode) via Spring Data JPA | Zero-install persistence for users, games, scores; swappable for MySQL/Postgres by changing 3 properties |
| Volatile / shared state | Redis (Docker container) | Online-presence registry, pending challenges with TTL, active-game session state |
| Frontend | Plain HTML/CSS/JS served by Spring Boot | "Simple UI" requirement; no build toolchain |

## 3. High-level component diagram

```
┌────────────────────────────  Browser (static JS)  ───────────────────────────┐
│  login.html ──► REST /api/auth/**          lobby+game (app.js)               │
│                                            │  STOMP over WebSocket /ws       │
└────────────────────────────────────────────┼──────────────────────────────────┘
                                             │
┌────────────────────────────  Spring Boot  ─┼──────────────────────────────────┐
│  SecurityFilterChain (JWT filter)          │                                  │
│                                            ▼                                  │
│  AuthController      WebSocketConfig + ChannelInterceptor (JWT on CONNECT)    │
│  AuthService         LobbyController / GameController (STOMP endpoints)       │
│       │                     │                       │                         │
│       ▼                     ▼                       ▼                         │
│  UserService         MatchmakingService        GameService ──► Chess ENGINE   │
│       │                     │                       │          (pure Java,    │
│       ▼                     ▼                       ▼           no Spring)    │
│  H2 (JPA): users,    Redis: online players,   H2: finished games,             │
│  accumulated scores  pending challenges,      score updates                   │
│                      active game state                                        │
└────────────────────────────────────────────────────────────────────────────────┘
```

## 4. Package layout (planned)

```
com.chessarena
├── ChessArenaApplication.java
├── config/          SecurityConfig, WebSocketConfig, RedisConfig
├── auth/            AuthController, AuthService, JwtService, JwtAuthFilter, DTOs
├── user/            User (JPA entity), UserRepository, UserService
├── engine/          ← pure-Java chess engine, ZERO Spring dependencies
│   ├── core/        Board, Position, Move, MoveResult, GameStatus, Color
│   ├── pieces/      Piece (abstract) + Pawn, Rook, Knight, Bishop, Queen, King
│   └── game/        ChessGame (facade over engine)
├── presence/        OnlinePlayerService (Redis-backed)
├── matchmaking/     Challenge, MatchmakingService
├── game/            GameSession, GameService, GameRecord (JPA), GameRepository
└── ws/              STOMP controllers, WebSocket event listeners, message DTOs
```

**Key boundary:** the `engine` package knows nothing about Spring, WebSockets, or
persistence. It is a self-contained domain model — this is deliberate separation
of concerns and makes the engine unit-testable in isolation.

## 5. The matchmaking flow (sequence)

```
PlayerA                    Server                              All online players
   │  STOMP: /app/lobby/find-opponent │                                │
   ├──────────────────────────────────►                                │
   │                                  │ store challenge in Redis (TTL 60s)
   │                                  ├── broadcast /topic/lobby/challenges ──►
   │                                  │                                │
   │                                  ◄── STOMP: /app/lobby/accept ────┤ PlayerB
   │                                  │ atomic claim in Redis (first wins)
   │                                  │ create GameSession, assign colors
   │  ◄── /user/queue/game-start ─────┤─── /user/queue/game-start ────►│
   │                                  │                                │
   │  ◄────── moves via /app/game/{id}/move  +  /topic/game/{id} ─────►│
   │                                  │ every move validated by engine │
   │                                  │ on game end: persist + scores  │
```

## 6. Scoring model

| Result | Points |
|---|---|
| Win | +3 |
| Draw (stalemate, insufficient material) | +1 |
| Loss | 0 |

Scores accumulate on the `users` table (`score`, `wins`, `losses`, `draws`) and a
leaderboard endpoint exposes the ranking.

## 7. OOP commitments

The codebase is explicitly written to demonstrate the four pillars plus SOLID.
The full mapping (principle → exact class/file) lives in
[09-oop-principles.md](09-oop-principles.md). Highlights:

- **Abstraction / Inheritance / Polymorphism:** abstract `Piece` with one subclass
  per chess piece; the board asks any piece for its legal moves without knowing
  its concrete type.
- **Encapsulation:** `Board` never exposes its internal array; all mutation goes
  through validated methods.
- **Single Responsibility:** engine vs. matchmaking vs. persistence vs. transport
  are separate packages with one reason to change each.
- **Dependency Inversion:** services depend on Spring Data repository interfaces
  and on the `ChessGame` facade, never on concrete storage details.

## 8. Document index

| Doc | Content |
|---|---|
| [01-project-setup.md](01-project-setup.md) | How the skeleton was generated, dependency choices |
| [02-chess-engine.md](02-chess-engine.md) | Engine design, rules coverage, class diagram |
| [03-authentication.md](03-authentication.md) | JWT flow, security filter chain |
| [04-persistence.md](04-persistence.md) | Entities, repositories, schema |
| [05-redis.md](05-redis.md) | What lives in Redis and why |
| [06-websocket-protocol.md](06-websocket-protocol.md) | Every STOMP destination + message shape |
| [07-matchmaking-and-game-flow.md](07-matchmaking-and-game-flow.md) | Challenge → accept → game lifecycle |
| [08-frontend.md](08-frontend.md) | Pages, UI flow |
| [09-oop-principles.md](09-oop-principles.md) | Principle-by-principle mapping to code |
| [10-setup-and-run.md](10-setup-and-run.md) | How to run everything locally |
