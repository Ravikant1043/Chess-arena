package com.chessarena.ws;

import com.chessarena.engine.game.ChessGame;
import com.chessarena.game.GameSession;
import com.chessarena.ws.messages.GameMessages.ChatMessage;
import com.chessarena.ws.messages.GameMessages.GameError;
import com.chessarena.ws.messages.GameMessages.GameOver;
import com.chessarena.ws.messages.GameMessages.GameStart;
import com.chessarena.ws.messages.GameMessages.GameState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Composes and publishes game-channel messages. Game state is broadcast on
 * {@code /topic/game/{gameId}} (both players subscribe); personal errors and the
 * game-start handshake go to the individual player's {@code /user/queue/game}.
 */
@Component
public class GameBroadcaster {

    private final SimpMessagingTemplate messaging;

    public GameBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Notifies each matched player privately that their game has started (with the initial board). */
    public void sendGameStart(GameSession session) {
        sendStartTo(session.getWhiteUsername(), session, "WHITE", session.getBlackUsername());
        sendStartTo(session.getBlackUsername(), session, "BLACK", session.getWhiteUsername());
    }

    private void sendStartTo(String user, GameSession s, String color, String opponent) {
        messaging.convertAndSendToUser(user, "/queue/game",
                new GameStart(s.getGameId(), s.getWhiteUsername(), s.getBlackUsername(), color, opponent,
                        s.getGame().boardGrid(), s.getGame().sideToMove().name()));
    }

    /** Broadcasts the current board/turn/status to both players after a move. */
    public void broadcastState(GameSession session, String lastMoveUci, String lastMoveText) {
        ChessGame game = session.getGame();
        GameState state = new GameState(
                session.getGameId(),
                game.boardGrid(),
                game.sideToMove().name(),
                game.status().name(),
                lastMoveUci,
                lastMoveText,
                game.moveHistory());
        messaging.convertAndSend("/topic/game/" + session.getGameId(), state);
    }

    /** Broadcasts a player's chat message to both players. */
    public void broadcastChat(GameSession session, String sender, String text) {
        messaging.convertAndSend("/topic/game/" + session.getGameId(),
                new ChatMessage(session.getGameId(), sender, text));
    }

    /** Broadcasts the terminal result to both players. */
    public void broadcastGameOver(GameSession session, String winnerUsername, String endReason) {
        GameOver over = new GameOver(
                session.getGameId(),
                winnerUsername,
                endReason,
                session.getGame().boardGrid());
        messaging.convertAndSend("/topic/game/" + session.getGameId(), over);
    }

    /** Sends a private error to a single player. */
    public void sendError(String username, String message) {
        messaging.convertAndSendToUser(username, "/queue/game", new GameError(message));
    }
}
