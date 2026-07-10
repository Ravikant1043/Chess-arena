package com.chessarena.user;

import com.chessarena.game.GameRecord;
import com.chessarena.game.GameRecordRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for users: registration, authentication lookups, the leaderboard,
 * and — importantly — the transactional accumulation of scores when a game finishes.
 *
 * <p>Depends only on the repository <i>interfaces</i> and the {@link PasswordEncoder}
 * abstraction (constructor injection), never on concrete implementations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final GameRecordRepository gameRecordRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       GameRecordRepository gameRecordRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new player with a BCrypt-hashed password.
     *
     * @throws UsernameTakenException if the username already exists.
     */
    @Transactional
    public User register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        User user = new User(username, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /** Verifies a login attempt; returns the user only if the password matches. */
    @Transactional(readOnly = true)
    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Unknown user or bad password"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Unknown user or bad password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No such user: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> leaderboard() {
        return userRepository.findTop20ByOrderByScoreDescWinsDesc();
    }

    /**
     * Records a finished game: persists a {@link GameRecord} and updates both players'
     * accumulated scores atomically, in one transaction.
     */
    @Transactional
    public void recordFinishedGame(String whiteUsername, String blackUsername,
                                   GameRecord.Outcome outcome, String endReason, String movesUci) {
        User white = requireByUsername(whiteUsername);
        User black = requireByUsername(blackUsername);

        switch (outcome) {
            case WHITE_WINS -> {
                white.recordWin();
                black.recordLoss();
            }
            case BLACK_WINS -> {
                black.recordWin();
                white.recordLoss();
            }
            case DRAW -> {
                white.recordDraw();
                black.recordDraw();
            }
        }

        userRepository.save(white);
        userRepository.save(black);
        gameRecordRepository.save(
                new GameRecord(whiteUsername, blackUsername, outcome, endReason, movesUci));
    }
}
