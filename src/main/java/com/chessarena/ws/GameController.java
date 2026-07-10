package com.chessarena.ws;

import com.chessarena.engine.core.Position;
import com.chessarena.game.GameResult;
import com.chessarena.game.GameService;
import com.chessarena.game.GameSession;
import com.chessarena.ws.messages.GameMessages.ChatRequest;
import com.chessarena.ws.messages.GameMessages.MoveRequest;
import com.chessarena.ws.messages.GameMessages.ResignRequest;
import com.chessarena.ws.messages.GameMessages.TargetsRequest;
import com.chessarena.ws.messages.GameMessages.TargetsResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * STOMP endpoints for an in-progress game.
 *
 * <p>Client destinations (prefix {@code /app}):
 * <ul>
 *   <li>{@code /app/game/move} — submit a move ({@code {gameId, uci}})</li>
 *   <li>{@code /app/game/resign} — resign the current game</li>
 *   <li>{@code /app/game/chat} — send a chat message ({@code {gameId, text}})</li>
 *   <li>{@code /app/game/targets} — ask for legal destinations of a piece ({@code {gameId, square}})</li>
 * </ul>
 * Every move is validated server-side by the engine before any broadcast; illegal moves
 * are answered privately and the board is never mutated.
 */
@Controller
public class GameController {

    private static final int MAX_CHAT_LENGTH = 300;

    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;
    private final LobbyBroadcaster lobbyBroadcaster;
    private final SimpMessagingTemplate messaging;

    public GameController(GameService gameService, GameBroadcaster gameBroadcaster,
                          LobbyBroadcaster lobbyBroadcaster, SimpMessagingTemplate messaging) {
        this.gameService = gameService;
        this.gameBroadcaster = gameBroadcaster;
        this.lobbyBroadcaster = lobbyBroadcaster;
        this.messaging = messaging;
    }

    @MessageMapping("/game/move")
    public void move(@Payload MoveRequest request, Principal principal) {
        GameResult result = gameService.applyMove(request.gameId(), principal.getName(), request.uci());
        if (!result.accepted()) {
            gameBroadcaster.sendError(principal.getName(), result.rejectionReason());
            return;
        }

        gameBroadcaster.broadcastState(result.session(), request.uci(), result.moveDescription());
        if (result.gameOver()) {
            gameBroadcaster.broadcastGameOver(result.session(), result.winnerUsername(), result.endReason());
            lobbyBroadcaster.broadcastOnlinePlayers(); // clear the in-game flags
        }
    }

    @MessageMapping("/game/resign")
    public void resign(@Payload ResignRequest request, Principal principal) {
        GameResult result = gameService.resign(principal.getName());
        if (!result.accepted()) {
            gameBroadcaster.sendError(principal.getName(), result.rejectionReason());
            return;
        }
        gameBroadcaster.broadcastGameOver(result.session(), result.winnerUsername(), result.endReason());
        lobbyBroadcaster.broadcastOnlinePlayers();
    }

    /** Relays a chat message to both players, after checking the sender belongs to the game. */
    @MessageMapping("/game/chat")
    public void chat(@Payload ChatRequest request, Principal principal) {
        String sender = principal.getName();
        Optional<GameSession> session = gameService.find(request.gameId());
        if (session.isEmpty() || !session.get().hasPlayer(sender)) {
            return; // silently drop chat for games the sender isn't part of
        }
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > MAX_CHAT_LENGTH) {
            text = text.substring(0, MAX_CHAT_LENGTH);
        }
        gameBroadcaster.broadcastChat(session.get(), sender, text);
    }

    /** Answers privately with the legal destination squares for the piece on the clicked square. */
    @MessageMapping("/game/targets")
    public void targets(@Payload TargetsRequest request, Principal principal) {
        String username = principal.getName();
        Optional<GameSession> session = gameService.find(request.gameId());
        if (session.isEmpty() || !session.get().hasPlayer(username)) {
            return;
        }
        List<String> targets;
        try {
            targets = session.get().getGame().legalTargets(Position.fromAlgebraic(request.square()));
        } catch (IllegalArgumentException ex) {
            return; // malformed square — ignore
        }
        messaging.convertAndSendToUser(username, "/queue/game",
                new TargetsResponse(request.square(), targets));
    }
}
