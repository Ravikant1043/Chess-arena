package com.chessarena.game;

import com.chessarena.engine.core.Color;
import com.chessarena.engine.core.GameStatus;
import com.chessarena.engine.core.Move;
import com.chessarena.engine.core.MoveResult;
import com.chessarena.engine.game.ChessGame;
import com.chessarena.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns every live {@link GameSession}: creation with color assignment, server-side move
 * validation via the engine, and end-of-game persistence + score accumulation.
 *
 * <p>Two indexes are maintained — by game id and by username — so a disconnect can find
 * and forfeit a player's in-progress game in O(1).
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final ConcurrentMap<String, GameSession> gamesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> gameIdByUser = new ConcurrentHashMap<>();

    private final UserService userService;

    public GameService(UserService userService) {
        this.userService = userService;
    }

    /** Creates a game between two matched players, assigning colors by coin-flip. */
    public GameSession createGame(String playerA, String playerB) {
        boolean aIsWhite = ThreadLocalRandom.current().nextBoolean();
        String white = aIsWhite ? playerA : playerB;
        String black = aIsWhite ? playerB : playerA;

        String gameId = UUID.randomUUID().toString();
        GameSession session = new GameSession(gameId, white, black);
        gamesById.put(gameId, session);
        gameIdByUser.put(white, gameId);
        gameIdByUser.put(black, gameId);

        log.info("Game {} created: white={}, black={}", gameId, white, black);
        return session;
    }

    public Optional<GameSession> find(String gameId) {
        return Optional.ofNullable(gamesById.get(gameId));
    }

    public Optional<String> activeGameIdOf(String username) {
        return Optional.ofNullable(gameIdByUser.get(username));
    }

    /**
     * Validates and applies a move. Enforces (a) the player belongs to the game and
     * (b) it is their turn, before deferring to the engine for chess-rule legality.
     */
    public synchronized GameResult applyMove(String gameId, String username, String uci) {
        GameSession session = gamesById.get(gameId);
        if (session == null) {
            return GameResult.rejected(null, "Game not found");
        }
        if (!session.hasPlayer(username)) {
            return GameResult.rejected(session, "You are not a player in this game");
        }
        if (!session.isTurnOf(username)) {
            return GameResult.rejected(session, "It is not your turn");
        }

        ChessGame game = session.getGame();
        MoveResult result;
        try {
            result = game.submitMove(Move.fromUci(uci));
        } catch (IllegalArgumentException ex) {
            return GameResult.rejected(session, "Malformed move: " + uci);
        }

        if (!result.legal()) {
            return GameResult.rejected(session, result.message());
        }

        if (result.status().isGameOver()) {
            return finish(session, result.status(), result.winner(), result.description());
        }
        return GameResult.ongoing(session, result.description());
    }

    /** Handles a resignation: the opponent wins. */
    public synchronized GameResult resign(String username) {
        String gameId = gameIdByUser.get(username);
        if (gameId == null) {
            return GameResult.rejected(null, "You have no active game");
        }
        GameSession session = gamesById.get(gameId);
        String winner = session.opponentOf(username);
        Color winnerColor = session.colorOf(winner);
        return finishWithWinner(session, winner, winnerColor, "RESIGNATION", null);
    }

    /**
     * Handles a disconnect: if the player was mid-game, the opponent wins by forfeit.
     *
     * @return the finished game (for broadcasting), or empty if the player had no game.
     */
    public synchronized Optional<GameResult> handleDisconnect(String username) {
        String gameId = gameIdByUser.get(username);
        if (gameId == null) {
            return Optional.empty();
        }
        GameSession session = gamesById.get(gameId);
        if (session == null) {
            gameIdByUser.remove(username);
            return Optional.empty();
        }
        String winner = session.opponentOf(username);
        Color winnerColor = session.colorOf(winner);
        return Optional.of(finishWithWinner(session, winner, winnerColor, "DISCONNECT", null));
    }

    // ------------------------------------------------- game completion

    /** Finish via a natural engine result (checkmate/stalemate/draw). */
    private GameResult finish(GameSession session, GameStatus status, Color winnerColor,
                              String moveDescription) {
        String winnerUsername = switch (winnerColor) {
            case WHITE -> session.getWhiteUsername();
            case BLACK -> session.getBlackUsername();
            case null -> null; // stalemate / draw
        };
        return finishWithWinner(session, winnerUsername, winnerColor, status.name(), moveDescription);
    }

    /** Shared completion path: persist the record, accumulate scores, drop the session. */
    private GameResult finishWithWinner(GameSession session, String winnerUsername,
                                        Color winnerColor, String endReason,
                                        String moveDescription) {
        GameRecord.Outcome outcome;
        if (winnerColor == Color.WHITE) {
            outcome = GameRecord.Outcome.WHITE_WINS;
        } else if (winnerColor == Color.BLACK) {
            outcome = GameRecord.Outcome.BLACK_WINS;
        } else {
            outcome = GameRecord.Outcome.DRAW;
        }

        String movesUci = String.join(" ", session.getGame().moveHistory());
        userService.recordFinishedGame(
                session.getWhiteUsername(), session.getBlackUsername(),
                outcome, endReason, movesUci);

        cleanUp(session);
        log.info("Game {} finished: outcome={}, reason={}", session.getGameId(), outcome, endReason);
        return GameResult.finished(session, winnerUsername, endReason, moveDescription);
    }

    private void cleanUp(GameSession session) {
        gamesById.remove(session.getGameId());
        gameIdByUser.remove(session.getWhiteUsername());
        gameIdByUser.remove(session.getBlackUsername());
    }
}
