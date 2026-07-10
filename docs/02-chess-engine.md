# Chess Engine

> Package `com.chessarena.engine` — pure Java, **zero Spring / web / persistence
> dependencies**. It is the domain core and is unit-tested in complete isolation
> (see `ChessEngineTest`, 5/5 passing).

## Why the engine is standalone

Server-authoritative multiplayer means the server, not the client, decides what is
legal. Keeping the engine free of any framework gives us:

- **Testability** — rules are verified with plain JUnit, no Spring context to boot.
- **Reusability** — the same engine could power a REST API, a CLI, or an AI trainer.
- **A clean dependency direction** — the web and matchmaking layers depend on the
  engine; the engine depends on nothing of ours (Dependency Inversion).

## Class map

```
engine/
├── core/
│   ├── Color            enum WHITE/BLACK; knows pawn direction, promotion rank, opposite()
│   ├── PieceType        enum PAWN..KING; material value + notation letter
│   ├── Position         record(file, rank); immutable square, off-board-safe offset()
│   ├── Move             record(from, to, promotion); UCI parse/format
│   ├── GameStatus       enum IN_PROGRESS/CHECK/CHECKMATE/STALEMATE/DRAW
│   ├── MoveResult       record; legal? + resulting status + winner
│   ├── Board            complete position; applyMove(), copy(), isSquareAttacked()
│   └── MoveGenerator    pseudo-legal → legal by king-safety filter
├── pieces/
│   ├── Piece (abstract) attacks(), pseudoLegalMoves(), copy(), code()
│   ├── Pawn             double-step, en passant, promotion (overrides pseudoLegalMoves)
│   ├── Knight, Bishop, Rook, Queen
│   └── King             one-step + castling (overrides pseudoLegalMoves)
└── game/
    └── ChessGame        FACADE: submitMove(), boardGrid(), legalTargets(), status()
```

## How a move is validated (the important part)

1. `ChessGame.submitMove(move)` asks `MoveGenerator.legalMoves(board)` for the full
   set of legal moves for the side to move.
2. `MoveGenerator` gets each piece's **pseudo-legal** moves (movement rules, ignoring
   check), then simulates each on a `board.copy()` and discards any that leave the
   mover's own king attacked (`isKingSafeAfter`).
3. The requested move is matched against that set (promotion defaults to queen if the
   client omits it). No match → `MoveResult.illegal(...)`, board untouched.
4. On a match the move is applied and the new `GameStatus` computed: no legal replies +
   in check → **checkmate**; no legal replies + not in check → **stalemate**;
   insufficient material or the fifty-move rule → **draw**; otherwise **check** or
   **in-progress**.

## Rules implemented

| Rule | Where |
|---|---|
| Piece movement (all six) | each `Piece` subclass `attacks()` / `pseudoLegalMoves()` |
| Blocked sliding paths | `Piece.slide()` ray-cast, stops at first blocker |
| Captures | base `pseudoLegalMoves` filters friendly-occupied targets |
| Check / checkmate / stalemate | `Board.isInCheck`, `ChessGame.computeStatus` |
| Castling (both sides, all safety rules) | `King.addCastlingMoves` + `Board.applyMove` rook relocation |
| En passant | `Pawn.pseudoLegalMoves` + `Board.applyMove` + `enPassantTarget` tracking |
| Promotion (Q/R/B/N) | `Pawn.addForwardOrPromotion` + `Board.applyMove` |
| Fifty-move & insufficient-material draws | `ChessGame.computeStatus` |

## Verified behaviour (`ChessEngineTest`)

| Test | Asserts |
|---|---|
| `initialPositionHasTwentyLegalMoves` | opening position → exactly 20 legal moves |
| `rejectsIllegalMove` | `e2e5` rejected |
| `foolsMateIsCheckmate` | `1.f3 e5 2.g4 Qh4#` → `CHECKMATE`, game over |
| `whiteCanCastleKingside` | `O-O` moves king to g1 **and** rook to f1 |
| `pawnPromotionProducesQueen` | pawn capturing to a8 becomes a white queen |

Run them with:

```bash
./mvnw test -Dtest=ChessEngineTest
```

## OOP principles on display here

- **Abstraction:** `Piece` defines *what* every piece can do; callers never switch on type.
- **Inheritance:** six concrete pieces share `pseudoLegalMoves` and `slide` from `Piece`.
- **Polymorphism:** `Board.isSquareAttacked` and `MoveGenerator` call `piece.attacks(...)`
  and the right subclass logic runs — adding a new piece variant would require no change
  to either.
- **Encapsulation:** `Board`'s grid is private; the only way to change it is the validated
  `applyMove`, and `Piece.hasMoved` flips only via `markMoved()`.

Full principle-to-code mapping across the whole project: [09-oop-principles.md](09-oop-principles.md).
