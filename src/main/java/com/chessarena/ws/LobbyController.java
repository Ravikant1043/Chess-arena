package com.chessarena.ws;

import com.chessarena.game.GameService;
import com.chessarena.game.GameSession;
import com.chessarena.matchmaking.Challenge;
import com.chessarena.matchmaking.MatchmakingService;
import com.chessarena.ws.messages.GameMessages.AcceptRequest;
import com.chessarena.ws.messages.GameMessages.CancelRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

/**
 * STOMP endpoints for the lobby: broadcasting a "find opponent" challenge to all online
 * players, and resolving accepts (first-accept-wins) into a live game.
 *
 * <p>Client destinations (prefix {@code /app}):
 * <ul>
 *   <li>{@code /app/lobby/find-opponent} — create + broadcast a challenge</li>
 *   <li>{@code /app/lobby/accept} — accept a challenge</li>
 *   <li>{@code /app/lobby/cancel} — cancel your own challenge</li>
 * </ul>
 */
@Controller
public class LobbyController {

    private static final Logger log = LoggerFactory.getLogger(LobbyController.class);

    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private final LobbyBroadcaster lobbyBroadcaster;
    private final GameBroadcaster gameBroadcaster;

    public LobbyController(MatchmakingService matchmakingService, GameService gameService,
                           LobbyBroadcaster lobbyBroadcaster, GameBroadcaster gameBroadcaster) {
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
        this.lobbyBroadcaster = lobbyBroadcaster;
        this.gameBroadcaster = gameBroadcaster;
    }

    /** A player clicks "Find Opponent": create an open challenge and broadcast it to the lobby. */
    @MessageMapping("/lobby/find-opponent")
    public void findOpponent(Principal principal) {
        String challenger = principal.getName();
        // A player already in a game shouldn't be issuing challenges.
        if (gameService.activeGameIdOf(challenger).isPresent()) {
            gameBroadcaster.sendError(challenger, "You are already in a game");
            return;
        }
        Challenge challenge = matchmakingService.createChallenge(challenger);
        log.info("{} is looking for an opponent (challenge {})", challenger, challenge.getId());
        lobbyBroadcaster.broadcastChallenge(challenge);
    }

    /** Another online player accepts. The first valid accept wins and starts the game. */
    @MessageMapping("/lobby/accept")
    public void accept(@Payload AcceptRequest request, Principal principal) {
        String accepter = principal.getName();
        if (gameService.activeGameIdOf(accepter).isPresent()) {
            gameBroadcaster.sendError(accepter, "You are already in a game");
            return;
        }

        Optional<Challenge> claimed = matchmakingService.accept(request.challengeId(), accepter);
        if (claimed.isEmpty()) {
            gameBroadcaster.sendError(accepter, "Challenge is no longer available");
            return;
        }

        Challenge challenge = claimed.get();
        GameSession session = gameService.createGame(challenge.getChallenger(), accepter);

        // Tell the lobby the challenge is gone, start the game for both, refresh presence flags.
        lobbyBroadcaster.broadcastChallengeClosed(challenge.getId(), "matched");
        gameBroadcaster.sendGameStart(session);
        gameBroadcaster.broadcastState(session, null, null);
        lobbyBroadcaster.broadcastOnlinePlayers();
        log.info("Match made: {} (challenger) vs {} (accepter) -> game {}",
                challenge.getChallenger(), accepter, session.getGameId());
    }

    /** The challenger cancels before anyone accepts. */
    @MessageMapping("/lobby/cancel")
    public void cancel(@Payload CancelRequest request, Principal principal) {
        matchmakingService.cancel(request.challengeId());
        lobbyBroadcaster.broadcastChallengeClosed(request.challengeId(), "cancelled");
    }
}
