# Persistence

> Packages `com.chessarena.user` and `com.chessarena.game`. Storage is **H2 in file
> mode**, so data survives restarts. Swapping to MySQL/PostgreSQL is a matter of changing
> the datasource URL/driver and adding the JDBC dependency — no code changes.

## Entities

### `User` (`users` table)
| Column | Type | Note |
|---|---|---|
| id | identity PK | |
| username | varchar(30), unique, not null | |
| passwordHash | varchar, not null | BCrypt hash |
| score | int | accumulated points |
| wins / losses / draws | int | tallies |
| createdAt | timestamp | |
| version | bigint | `@Version` optimistic lock |

The score/tally fields are mutated only through `recordWin()`, `recordLoss()`,
`recordDraw()` — the scoring rule (win = +3, draw = +1, loss = +0) is encapsulated inside
the entity so score can never drift out of sync with the tallies.

### `GameRecord` (`game_records` table)
Stores every finished game: `whiteUsername`, `blackUsername`, `outcome`
(`WHITE_WINS`/`BLACK_WINS`/`DRAW`), `endReason` (`CHECKMATE`/`STALEMATE`/`DRAW`/
`RESIGNATION`/`DISCONNECT`), the full `movesUci` list, and `playedAt`.

## Repositories (Spring Data JPA)

- `UserRepository` — `findByUsername`, `existsByUsername`,
  `findTop20ByOrderByScoreDescWinsDesc` (leaderboard).
- `GameRecordRepository` —
  `findTop20ByWhiteUsernameOrBlackUsernameOrderByPlayedAtDesc` (a player's history).

Declaring only interfaces and letting Spring synthesise the queries is Dependency
Inversion: services depend on the abstraction, never on JDBC.

## Transactional score accumulation

`UserService.recordFinishedGame(...)` runs in a single `@Transactional` method:

1. Load both players.
2. Apply the result (`recordWin/Loss/Draw`).
3. Save both users **and** the `GameRecord`.

Because it is one transaction, a game result is recorded atomically — you never get a
persisted game with only one player's score updated. The `@Version` column guards against
two concurrent updates to the same user clobbering each other.

## Verified

The end-to-end test (`scratchpad/e2e.mjs`, run against the live server) played a full game
to checkmate and then read `/api/me` for both players: winner `score=3, wins=1`, loser
`score=0, losses=1` — confirming persistence and accumulation.
