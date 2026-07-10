package com.chessarena.user;

import com.chessarena.game.GameRecord;
import com.chessarena.game.GameRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Read-only REST endpoints backing the lobby UI: the caller's own profile, the global
 * leaderboard, and the caller's recent game history. All require a valid JWT.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    public record ProfileDto(String username, int score, int wins, int losses, int draws, int gamesPlayed) {
    }

    public record LeaderboardEntry(int rank, String username, int score, int wins, int losses, int draws) {
    }

    public record HistoryEntry(String white, String black, String outcome, String endReason, String playedAt) {
    }

    private final UserService userService;
    private final GameRecordRepository gameRecordRepository;

    public UserController(UserService userService, GameRecordRepository gameRecordRepository) {
        this.userService = userService;
        this.gameRecordRepository = gameRecordRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDto> me(Principal principal) {
        User user = userService.requireByUsername(principal.getName());
        return ResponseEntity.ok(new ProfileDto(
                user.getUsername(), user.getScore(), user.getWins(),
                user.getLosses(), user.getDraws(), user.gamesPlayed()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> leaderboard() {
        List<User> top = userService.leaderboard();
        List<LeaderboardEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            User u = top.get(i);
            entries.add(new LeaderboardEntry(i + 1, u.getUsername(), u.getScore(),
                    u.getWins(), u.getLosses(), u.getDraws()));
        }
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryEntry>> history(Principal principal) {
        String me = principal.getName();
        List<GameRecord> records =
                gameRecordRepository.findTop20ByWhiteUsernameOrBlackUsernameOrderByPlayedAtDesc(me, me);
        List<HistoryEntry> entries = records.stream()
                .map(r -> new HistoryEntry(
                        r.getWhiteUsername(), r.getBlackUsername(),
                        r.getOutcome().name(), r.getEndReason(), r.getPlayedAt().toString()))
                .toList();
        return ResponseEntity.ok(entries);
    }
}
