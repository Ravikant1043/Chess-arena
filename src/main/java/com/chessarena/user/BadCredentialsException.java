package com.chessarena.user;

/** Thrown when a login attempt fails (unknown user or wrong password). */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
