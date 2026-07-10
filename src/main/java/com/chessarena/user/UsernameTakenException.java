package com.chessarena.user;

/** Thrown when registration is attempted with an already-registered username. */
public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
