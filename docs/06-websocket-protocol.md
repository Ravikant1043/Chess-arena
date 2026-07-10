# WebSocket Protocol (STOMP)

> Package `com.chessarena.ws` + `config.WebSocketConfig`.

## Connection

- **Endpoint:** `/ws` (SockJS enabled; raw WebSocket at `/ws/websocket`).
- **Auth:** the JWT is sent on the STOMP `CONNECT` frame as a native header
  `Authorization: Bearer <jwt>`. A `ChannelInterceptor` validates it and attaches a
  `StompPrincipal(username)` to the session. An invalid/missing token rejects the connect.
- **Destination prefixes:** `/app` (client→server), `/topic` (broadcast),
  `/queue` + `/user` (per-user).

## Channels

| Destination | Direction | Purpose |
|---|---|---|
| `/topic/lobby` | server→all | online-players list, new challenges, challenge-closed |
| `/user/queue/game` | server→one | game-start handshake, private errors |
| `/topic/game/{gameId}` | server→both players | board state + game-over |
| `/app/lobby/find-opponent` | client→server | create + broadcast a challenge |
| `/app/lobby/accept` | client→server | accept a challenge (`{challengeId}`) |
| `/app/lobby/cancel` | client→server | cancel own challenge (`{challengeId}`) |
| `/app/game/move` | client→server | submit a move (`{gameId, uci}`) |
| `/app/game/resign` | client→server | resign (`{gameId}`) |
| `/app/game/chat` | client→server | send a chat message (`{gameId, text}`, max 300 chars) |
| `/app/game/targets` | client→server | ask legal destinations for a piece (`{gameId, square}`) |

## Message shapes (server → client)

All outbound messages carry a `type` discriminator.

```jsonc
// /topic/lobby
{ "type":"ONLINE_PLAYERS", "players":[ {"username":"a","score":3,"inGame":false} ] }
{ "type":"CHALLENGE", "challengeId":"uuid", "challenger":"alice" }
{ "type":"CHALLENGE_CLOSED", "challengeId":"uuid", "reason":"matched" }

// /user/queue/game
{ "type":"GAME_START", "gameId":"uuid", "white":"alice", "black":"bob",
  "yourColor":"WHITE", "opponent":"bob" }
{ "type":"ERROR", "message":"It is not your turn" }

// /topic/game/{gameId}
{ "type":"GAME_STATE", "gameId":"uuid", "board":[[ "bR","bN",... ],...],
  "sideToMove":"BLACK", "status":"CHECK", "lastMove":"f2f3",
  "lastMoveText":"Pawn to f3", "moveHistory":["f2f3","e7e5"] }
{ "type":"CHAT", "gameId":"uuid", "sender":"alice", "text":"good luck!" }
{ "type":"GAME_OVER", "gameId":"uuid", "winner":"alice", "endReason":"CHECKMATE",
  "board":[...] }

// /user/queue/game (reply to /app/game/targets)
{ "type":"TARGETS", "square":"e2", "targets":["e3","e4"] }
```

`lastMoveText` is a spoken-style description built by the engine ("Rook to a4",
"Bishop takes c4 — check!", "King castles king-side", "Pawn to e8, promoted to Queen")
that the client shows in the game chat. Chat messages are relayed only if the sender is
a player in that game; text is trimmed and capped at 300 characters.

`board` is 8 rows × 8 files. Row 0 = rank 8 (Black's back rank), row 7 = rank 1. Each cell
is a 2-char code (`"wP"`, `"bK"`) or `null`. Moves are **UCI** strings (`e2e4`, `e7e8q`).

## Server-authoritative guarantee

Every `/app/game/move` is validated by the engine (`GameService.applyMove`) which checks:
the sender is a player in that game, it is their turn, and the move is chess-legal. Only
then is a new `GAME_STATE` broadcast. Illegal attempts get a private `ERROR` and the board
is unchanged — clients cannot cheat.

## Presence lifecycle

`WebSocketEventListener` reacts to raw STOMP events:
- **connect** → `markOnline` + broadcast `ONLINE_PLAYERS`.
- **disconnect** → forfeit any in-progress game (opponent wins by `DISCONNECT`), cancel
  open challenges, `markOffline`, broadcast `ONLINE_PLAYERS`.
