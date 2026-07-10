# Project Setup

## How the skeleton was generated

The project was bootstrapped from [start.spring.io](https://start.spring.io) as a Maven
project pinned to **Spring Boot 3.5.16** on **Java 21**, with these starters:

`web`, `websocket`, `security`, `data-jpa`, `h2`, `data-redis`, `validation`.

JWT support (`io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`) was added to `pom.xml` by hand.

## Build tooling

The Maven **wrapper** (`./mvnw`) is committed, so no global Maven install is needed —
only a JDK 21+.

```bash
./mvnw compile            # compile
./mvnw test               # run all tests (engine + context)
./mvnw spring-boot:run    # run the app on :8080
```

## Configuration (`application.properties`)

| Property | Value | Note |
|---|---|---|
| `server.port` | 8080 | |
| `spring.datasource.url` | `jdbc:h2:file:./data/chessarena;AUTO_SERVER=TRUE` | file-based → survives restarts |
| `spring.jpa.hibernate.ddl-auto` | `update` | schema auto-managed for dev |
| `spring.h2.console.enabled` | true | dev console at `/h2-console` |
| `spring.data.redis.host/port` | localhost:6379 | only used under the `redis` profile |
| `app.jwt.secret` | env `JWT_SECRET` or dev default | HMAC-SHA256 key |
| `app.jwt.expiration-ms` | 86400000 | 24h token validity |

## On Docker / Redis

Per the current scope, **Docker and Redis are not required to run the app.** Presence is
served by an in-memory store by default; the Redis-backed implementation is code-complete
and activates only under the `redis` Spring profile. See [05-redis.md](05-redis.md).

## Package structure

```
com.chessarena
├── config/       SecurityConfig, WebSocketConfig
├── auth/         JwtService, JwtAuthFilter, AuthController, dto/
├── user/         User, UserRepository, UserService, UserController
├── engine/       core/, pieces/, game/   (pure Java chess engine)
├── presence/     PresenceStore (+ InMemory / Redis impls)
├── matchmaking/  Challenge, MatchmakingService
├── game/         GameSession, GameService, GameRecord, GameResult, repo
└── ws/           StompPrincipal, controllers, broadcasters, messages/
```
