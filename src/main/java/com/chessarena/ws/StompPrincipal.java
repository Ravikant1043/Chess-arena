package com.chessarena.ws;

import java.security.Principal;

/**
 * A minimal {@link Principal} carrying the authenticated username, attached to a STOMP
 * session at CONNECT time. Enables {@code @MessageMapping} handlers to receive the caller
 * as a {@code Principal} and lets the server target a specific user with
 * {@code convertAndSendToUser(...)}.
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
