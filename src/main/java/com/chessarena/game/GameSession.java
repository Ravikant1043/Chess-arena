package com.chessarena.game;

import com.chessarena.engine.core.Color;
import com.chessarena.engine.game.ChessGame;

/**
 * A live game between two players: the engine plus the mapping of usernames to colors.
 *
 * <p>Wraps rather than extends {@link ChessGame} (composition over inheritance): a
 * session <i>has a</i> game, and adds the player/turn concerns the pure engine has no
 * business knowing about.
 */
public class GameSession {

    private final String gameId;
    private final String whiteUsername;
    private final String blackUsername;
    private final ChessGame game;

    public GameSession(String gameId, String whiteUsername, String blackUsername) {
        this.gameId = gameId;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.game = new ChessGame();
    }

    /** The color assigned to a username, or {@code null} if they are not in this game. */
    public Color colorOf(String username) {
        if (username.equals(whiteUsername)) {
            return Color.WHITE;
        }
        if (username.equals(blackUsername)) {
            return Color.BLACK;
        }
        return null;
    }

    /** @return true if it is currently {@code username}'s turn to move. */
    public boolean isTurnOf(String username) {
        Color color = colorOf(username);
        return color != null && color == game.sideToMove();
    }

    public String opponentOf(String username) {
        if (username.equals(whiteUsername)) {
            return blackUsername;
        }
        if (username.equals(blackUsername)) {
            return whiteUsername;
        }
        return null;
    }

    public boolean hasPlayer(String username) {
        return username.equals(whiteUsername) || username.equals(blackUsername);
    }

    public String getGameId() {
        return gameId;
    }

    public String getWhiteUsername() {
        return whiteUsername;
    }

    public String getBlackUsername() {
        return blackUsername;
    }

    public ChessGame getGame() {
        return game;
    }
}
