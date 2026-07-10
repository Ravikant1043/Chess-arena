# Matchmaking & Game Flow

> Packages `com.chessarena.matchmaking` and `com.chessarena.game`.

## The flow you asked for

> "Open the game → click Find Opponent → a request is sent to all online players → they
> accept or not → if an opponent matches, the game begins → score is stored and
> accumulated."

Mapped to the implementation:

```
1. Player opens the app, logs in, WebSocket connects  → they appear in ONLINE_PLAYERS
2. Player clicks "Find Opponent"                       → /app/lobby/find-opponent
      LobbyController creates a Challenge (OPEN) and BROADCASTS it to /topic/lobby
      → every online player sees "alice wants to play  [Accept]"
3. Any online player clicks Accept                     → /app/lobby/accept {challengeId}
      MatchmakingService.accept → Challenge.tryClaim (synchronized: FIRST ACCEPT WINS)
4. On a successful claim:
      GameService.createGame → colors assigned by coin-flip → GameSession created
      • /topic/lobby       CHALLENGE_CLOSED ("matched")   → others' Accept button vanishes
      • /user/queue/game   GAME_START to each player       → clients open the board
      • /topic/game/{id}   initial GAME_STATE
5. Players alternate /app/game/move; engine validates each; GAME_STATE broadcast
6. On checkmate/stalemate/draw/resign/disconnect:
      GameService.finish → UserService.recordFinishedGame (persist + accumulate scores)
      • /topic/game/{id}   GAME_OVER
      • /topic/lobby       refreshed ONLINE_PLAYERS (in-game flags cleared)
```

## First-accept-wins (the race that matters)

When a challenge is broadcast to many players, two might click Accept at nearly the same
time. Correctness rests on `Challenge.tryClaim(accepter)`:

```java
public synchronized boolean tryClaim(String accepter) {
    if (status != Status.OPEN || accepter.equals(challenger)) return false;
    this.status = Status.CLAIMED;
    this.claimedBy = accepter;
    return true;
}
```

`synchronized` makes the check-and-set atomic, so exactly one accepter transitions the
challenge from `OPEN` to `CLAIMED`; the loser receives `"Challenge is no longer available"`.

## Guards

- A player already in a game cannot issue or accept challenges.
- A move is rejected unless the sender belongs to the game **and** it is their turn
  (`GameSession.isTurnOf`) — before chess-rule validation even runs.
- Disconnect mid-game = forfeit: the opponent is awarded the win with reason `DISCONNECT`,
  and scores settle immediately.

## Color assignment

`GameService.createGame` coin-flips (`ThreadLocalRandom`) which of the two matched players
is White, so the challenger has no fixed advantage.

## Scoring

Handled in `UserService.recordFinishedGame` (one transaction): win = **+3**, draw = **+1**,
loss = **+0**, with `wins/losses/draws` tallies kept in step. See
[04-persistence.md](04-persistence.md).
