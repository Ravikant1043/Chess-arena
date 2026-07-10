package com.chessarena.ws;

import com.chessarena.game.GameResult;
import com.chessarena.game.GameService;
import com.chessarena.matchmaking.MatchmakingService;
import com.chessarena.presence.PresenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Optional;

/**
 * Turns raw WebSocket lifecycle events into presence and matchmaking side effects:
 *
 * <ul>
 *   <li><b>Connect:</b> mark the user online and refresh the lobby's player list.</li>
 *   <li><b>Disconnect:</b> mark offline, cancel any open challenges they created, and —
 *       if they were mid-game — forfeit it so the opponent wins and scores settle.</li>
 * </ul>
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceStore presenceStore;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private final LobbyBroadcaster lobbyBroadcaster;
    private final GameBroadcaster gameBroadcaster;

    public WebSocketEventListener(PresenceStore presenceStore, MatchmakingService matchmakingService,
                                  GameService gameService, LobbyBroadcaster lobbyBroadcaster,
                                  GameBroadcaster gameBroadcaster) {
        this.presenceStore = presenceStore;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
        this.lobbyBroadcaster = lobbyBroadcaster;
        this.gameBroadcaster = gameBroadcaster;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String username = usernameOf(event.getUser());
        if (username == null) {
            return;
        }
        presenceStore.markOnline(username);
        log.info("{} connected", username);
        lobbyBroadcaster.broadcastOnlinePlayers();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        String username = usernameOf(user);
        if (username == null) {
            return;
        }

        // Forfeit any in-progress game before we forget the player.
        Optional<GameResult> forfeit = gameService.handleDisconnect(username);
        forfeit.ifPresent(result ->
                gameBroadcaster.broadcastGameOver(result.session(), result.winnerUsername(), result.endReason()));

        matchmakingService.cancelChallengesBy(username);
        presenceStore.markOffline(username);
        log.info("{} disconnected", username);
        lobbyBroadcaster.broadcastOnlinePlayers();
    }

    private String usernameOf(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
