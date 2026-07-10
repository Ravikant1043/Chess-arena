# Setup & Run

## Prerequisites

- **JDK 21+** (`java -version`). Nothing else — the Maven wrapper (`./mvnw`) is included.
- Docker/Redis are **not** required (see [05-redis.md](05-redis.md)).

## Run

```bash
cd chess-arena
./mvnw spring-boot:run
```

Then open **http://localhost:8080**.

## Try it (two players)

1. Open http://localhost:8080 in a normal window → register `alice`, log in.
2. Open http://localhost:8080 in a **second browser or an incognito window** → register
   `bob`, log in.
3. In alice's window, click **Find Opponent**.
4. In bob's window, alice's challenge appears — click **Accept**.
5. Both windows switch to the board. Make moves by clicking a piece then its destination.
6. On game end, the banner shows the result and both players' scores update; the
   leaderboard refreshes.

## Tests

```bash
./mvnw test -Dtest=ChessEngineTest   # engine rule coverage (5 tests)
./mvnw test                          # everything
```

### Headless end-to-end check

With the server running, the script `scratchpad/e2e.mjs` (Node 21+, uses the vendored
STOMP client and Node's built-in WebSocket) drives two players through registration,
matchmaking, a full Fool's-mate game, and asserts the resulting scores. It prints
`✅ PASS` on success. This was used to verify the live system.

## Useful endpoints

| URL | What |
|---|---|
| `http://localhost:8080/` | the app |
| `POST /api/auth/register` · `/login` | auth |
| `GET /api/me` · `/leaderboard` · `/history` | profile / rankings / history (JWT required) |
| `http://localhost:8080/h2-console` | H2 DB console (JDBC URL `jdbc:h2:file:./data/chessarena`, user `sa`, no password) |

## Enabling Redis (optional)

```bash
docker run -p 6379:6379 redis:7-alpine        # or: brew install redis && redis-server
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
```

## Production notes

- Set a strong `JWT_SECRET` env var (≥ 32 bytes).
- Point the datasource at MySQL/PostgreSQL and set `ddl-auto=validate` with managed migrations.
- Disable the H2 console.
