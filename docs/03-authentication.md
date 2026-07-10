# Authentication & Security

> Packages `com.chessarena.auth` and `com.chessarena.config`.

## Model: stateless JWT

A player registers or logs in over REST and receives a **JSON Web Token**. The browser
stores it and sends it back two ways:

- REST calls: `Authorization: Bearer <token>` header.
- WebSocket: the token is passed on the STOMP `CONNECT` frame (see
  [06-websocket-protocol.md](06-websocket-protocol.md)).

No server-side session is kept — the token itself is the proof of identity, so the
backend scales horizontally without sticky sessions.

## Components

| Class | Responsibility |
|---|---|
| `JwtService` | Issue HMAC-SHA256 tokens (subject = username); validate + extract username |
| `JwtAuthFilter` | `OncePerRequestFilter`; reads the Bearer header, populates the `SecurityContext` |
| `SecurityConfig` | BCrypt encoder, stateless filter chain, public-endpoint allow-list, CORS |
| `AuthController` | `POST /api/auth/register`, `POST /api/auth/login` |
| `AuthDtos` | `RegisterRequest`, `LoginRequest`, `AuthResponse` (Bean-Validation annotated) |
| `UserService` | BCrypt hashing on register; password check on login |

## Endpoints

### `POST /api/auth/register`
```json
Request:  { "username": "magnus", "password": "secret123" }
Response: 201  { "token": "<jwt>", "username": "magnus", "score": 0 }
Errors:   409  { "error": "Username already taken: magnus" }
          400  validation failure (username 3-30 chars, password 6-100 chars)
```

### `POST /api/auth/login`
```json
Request:  { "username": "magnus", "password": "secret123" }
Response: 200  { "token": "<jwt>", "username": "magnus", "score": 42 }
Errors:   401  { "error": "Unknown user or bad password" }
```

## Security decisions

- **Passwords** are stored only as BCrypt hashes (`BCryptPasswordEncoder`), never in plaintext.
- **CSRF** is disabled — safe here because auth is via a Bearer token, not a cookie.
- **Session policy** is `STATELESS`; Spring Security creates no `HttpSession`.
- **Public allow-list:** `/api/auth/**`, `/ws/**` (handshake), static UI, and the dev-only
  H2 console. Everything else requires a valid token.
- The JWT secret comes from the `JWT_SECRET` env var in production
  (`app.jwt.secret` in `application.properties`); the checked-in value is a dev default only.

## OOP / design notes

- `UserService` depends on the `UserRepository` and `PasswordEncoder` **abstractions**
  (constructor injection) — Dependency Inversion.
- Scoring points (`WIN_POINTS`, `DRAW_POINTS`) and the counters live inside the `User`
  entity behind `recordWin/Loss/Draw()`; callers cannot desynchronise score from record
  (encapsulation of an invariant).
