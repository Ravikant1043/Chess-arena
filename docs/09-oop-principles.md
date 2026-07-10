# OOP Principles — Mapped to the Code

This project was written to demonstrate object-oriented design deliberately. Below, each
principle is tied to the exact class/file where it lives.

## The four pillars

### 1. Abstraction
- **`Piece`** (`engine/pieces/Piece.java`) — an abstract class defining *what* every chess
  piece can do (`attacks`, `pseudoLegalMoves`, `copy`) without saying how. Callers work
  with `Piece`, never a concrete type.
- **`PresenceStore`** (`presence/PresenceStore.java`) — an interface abstracting "who is
  online" away from whether that's stored in memory or Redis.
- **`ChessGame`** (`engine/game/ChessGame.java`) — a Facade abstracting the whole engine
  behind `submitMove` / `boardGrid` / `status`.

### 2. Encapsulation
- **`Board`** (`engine/core/Board.java`) — the `Piece[][]` grid is `private` and never
  exposed; the only mutation path is the validated `applyMove`.
- **`Piece.hasMoved`** — flips only through `markMoved()`, protecting castling/pawn rules.
- **`User`** (`user/User.java`) — score/win/loss/draw counters change only via
  `recordWin/Loss/Draw()`, keeping score consistent with the tallies.
- **`Challenge`** (`matchmaking/Challenge.java`) — state transitions are guarded inside
  `synchronized tryClaim/cancel`.

### 3. Inheritance
- **`Pawn`, `Knight`, `Bishop`, `Rook`, `Queen`, `King`** all extend `Piece`, reusing its
  `pseudoLegalMoves` template and the `slide` ray-casting helper. `Bishop`/`Rook`/`Queen`
  share `slide` with different direction vectors.

### 4. Polymorphism
- **`Board.isSquareAttacked`** and **`MoveGenerator.legalMoves`** call
  `piece.attacks(...)` / `piece.pseudoLegalMoves(...)` uniformly; the correct subclass
  behaviour runs. `Pawn` and `King` **override** `pseudoLegalMoves` for their special
  rules while everyone else uses the inherited version.

## SOLID

- **S — Single Responsibility:** `Board` (hold/mutate position) vs `MoveGenerator`
  (legality) vs `ChessGame` (game lifecycle) vs `MatchmakingService` (challenges) vs
  `GameService` (live games) vs `UserService` (accounts/scores). Each has one reason to
  change. The broadcasters (`LobbyBroadcaster`, `GameBroadcaster`) isolate message
  composition from controllers.
- **O — Open/Closed:** adding a new piece variant means a new `Piece` subclass — no edits
  to `Board`, `MoveGenerator`, or `ChessGame`.
- **L — Liskov Substitution:** any `Piece` subclass is usable wherever a `Piece` is
  expected; any `PresenceStore` impl is interchangeable. The E2E test passed with the
  in-memory store exactly as it would with Redis.
- **I — Interface Segregation:** `PresenceStore` exposes only the four presence
  operations; Spring Data repositories expose only the queries each caller needs.
- **D — Dependency Inversion:** services depend on abstractions — repository interfaces,
  `PasswordEncoder`, `PresenceStore`, the `ChessGame` facade — injected via constructors,
  never on concrete implementations.

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Facade** | `ChessGame` | one small surface over the engine's internals |
| **Strategy / polymorphism** | `Piece` subclasses | per-piece move rules behind one type |
| **Template method** | `Piece.pseudoLegalMoves` (base) + overrides | shared skeleton, specialised steps |
| **Composition over inheritance** | `GameSession` *has a* `ChessGame` | player/turn concerns kept out of the engine |
| **Value objects (immutability)** | `Position`, `Move` (records) | safe as map keys, no aliasing bugs |
| **Dependency injection** | every `@Service`/`@Component` | testability, loose coupling |
| **Profile-based substitution** | `InMemoryPresenceStore` / `RedisPresenceStore` | swap infra without touching callers |

## Immutability & concurrency

- `Position`, `Move`, `MoveResult`, and all WS message DTOs are Java `record`s
  (immutable value types).
- Shared mutable state uses concurrent structures (`ConcurrentHashMap`,
  `ConcurrentHashMap.newKeySet`) and `synchronized` where check-and-act atomicity is
  required (`Challenge.tryClaim`, `GameService.applyMove`).
