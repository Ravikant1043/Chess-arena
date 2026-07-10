package com.chessarena.ws;

import com.chessarena.game.GameService;
import com.chessarena.matchmaking.Challenge;
import com.chessarena.presence.PresenceStore;
import com.chessarena.user.User;
import com.chessarena.user.UserService;
import com.chessarena.ws.messages.LobbyMessages.ChallengeBroadcast;
import com.chessarena.ws.messages.LobbyMessages.ChallengeClosed;
import com.chessarena.ws.messages.LobbyMessages.OnlinePlayers;
import com.chessarena.ws.messages.LobbyMessages.PlayerInfo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single place that composes and publishes lobby messages. Centralising it keeps the
 * controllers and the connect/disconnect listener from duplicating message-building
 * logic (Single Responsibility).
 */
@Component
public class LobbyBroadcaster {

    private static final String LOBBY_TOPIC = "/topic/lobby";

    private final SimpMessagingTemplate messaging;
    private final PresenceStore presenceStore;
    private final UserService userService;
    private final GameService gameService;

    public LobbyBroadcaster(SimpMessagingTemplate messaging, PresenceStore presenceStore,
                            UserService userService, GameService gameService) {
        this.messaging = messaging;
        this.presenceStore = presenceStore;
        this.userService = userService;
        this.gameService = gameService;
    }

    /** Publishes the current online-players list (with scores and in-game flags) to the lobby. */
    public void broadcastOnlinePlayers() {
        List<PlayerInfo> players = presenceStore.onlineUsers().stream()
                .map(this::toPlayerInfo)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();
        messaging.convertAndSend(LOBBY_TOPIC, new OnlinePlayers(players));
    }

    private PlayerInfo toPlayerInfo(String username) {
        int score = 0;
        try {
            User user = userService.requireByUsername(username);
            score = user.getScore();
        } catch (RuntimeException ignored) {
            // Presence without a persisted user shouldn't happen; default score 0.
        }
        boolean inGame = gameService.activeGameIdOf(username).isPresent();
        return new PlayerInfo(username, score, inGame);
    }

    public void broadcastChallenge(Challenge challenge) {
        messaging.convertAndSend(LOBBY_TOPIC,
                new ChallengeBroadcast(challenge.getId(), challenge.getChallenger()));
    }

    public void broadcastChallengeClosed(String challengeId, String reason) {
        messaging.convertAndSend(LOBBY_TOPIC, new ChallengeClosed(challengeId, reason));
    }
}
