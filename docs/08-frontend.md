# Frontend

> Static files under `src/main/resources/static`, served by Spring Boot. No build step,
> no framework — plain HTML/CSS/vanilla JS, as requested ("keep the UI simple").

## Files

| File | Role |
|---|---|
| `index.html` | Single page with three views: auth, lobby, game |
| `css/style.css` | Dark theme, responsive board (CSS grid) |
| `js/app.js` | All client logic (auth, STOMP, lobby, board) |
| `js/sockjs.min.js` | SockJS client (vendored locally — no CDN at runtime) |
| `js/stomp.umd.min.js` | STOMP client (vendored locally) |

## Views and how they switch

1. **Auth** — login/register tabs. On success the JWT + username go into `localStorage`
   and the app auto-enters. On reload, a stored token logs the user straight back in.
2. **Lobby** — a "Find Opponent" button, live online-players list (with scores), open
   challenges (each with an Accept button), and a leaderboard.
3. **Game** — the board, whose-turn indicator, a WhatsApp-style chat, resign button, and
   an end-of-game banner with "Back to lobby".

## In-game chat

The sidebar hosts a chat box instead of a raw move list:

- **Player messages** render as bubbles with a circular avatar (the player's initial):
  the **current user on the left** (green), the **opponent on the right** (blue) —
  WhatsApp-style.
- **Move announcements** appear between the bubbles as centered system lines, e.g.
  "**bob**: Knight to c3" or "**alice**: Queen takes h4 — checkmate!". The text comes
  from the server (`lastMoveText` on `GAME_STATE`); the client works out the mover
  (the side that is *not* on turn now).
- Game start and game over are also announced in the chat.
- Sending: the input publishes to `/app/game/chat`; the server relays to both players.
  User text is HTML-escaped before rendering (no injection).

## Board rendering

- Built as an 8×8 CSS grid. Pieces are Unicode glyphs (`♚♛♜♝♞♟`) coloured white/black.
- **Orientation:** the board flips so the logged-in player's pieces are always at the
  bottom (White sees rank 1 at the bottom; Black sees rank 8 at the bottom).
- **Click-to-move with legal-move highlighting:** clicking one of your pieces selects it
  and asks the server (`/app/game/targets`) for its legal destinations; those squares get
  a dot marker (`.target`). Clicking a highlighted (or any) square sends the move as UCI
  over `/app/game/move`; clicking another of your pieces switches the selection. A pawn
  reaching the last rank auto-promotes to a queen (`...q`). If the server rejects the
  move, a private `ERROR` is shown and the board stays put.
- **Last move** is highlighted; **check** is shown in the turn line.

## Real-time wiring

On login, `app.js` opens one STOMP connection (JWT on CONNECT) and subscribes to
`/topic/lobby` and `/user/queue/game`. When a `GAME_START` arrives it subscribes to
`/topic/game/{gameId}` and renders every `GAME_STATE` the server broadcasts. REST calls
(`/api/me`, `/api/leaderboard`) refresh scores after each game.

## Auth/token handling

Every REST call sends `Authorization: Bearer <token>`. The token is the same one used for
the WebSocket handshake, so a single login authenticates both channels.
