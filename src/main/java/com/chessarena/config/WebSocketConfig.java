package com.chessarena.config;

import com.chessarena.auth.JwtService;
import com.chessarena.ws.StompPrincipal;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * STOMP-over-WebSocket configuration.
 *
 * <ul>
 *   <li>Handshake endpoint {@code /ws} (with SockJS fallback).</li>
 *   <li>Simple in-memory broker with destinations {@code /topic} (broadcast) and
 *       {@code /queue} (per-user); client sends are prefixed {@code /app}.</li>
 *   <li>An inbound {@link ChannelInterceptor} authenticates the STOMP {@code CONNECT}
 *       frame by validating the JWT passed in the {@code Authorization} header, then
 *       attaches a {@link StompPrincipal} so every later frame is tied to that user.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String username = resolveUsername(accessor);
                    if (username == null) {
                        throw new IllegalArgumentException("Missing or invalid JWT on STOMP CONNECT");
                    }
                    accessor.setUser(new StompPrincipal(username));
                }
                return message;
            }
        });
    }

    /** Reads {@code Authorization: Bearer <jwt>} from the CONNECT frame's native headers. */
    private String resolveUsername(StompHeaderAccessor accessor) {
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth == null || auth.isEmpty()) {
            return null;
        }
        String header = auth.get(0);
        if (!header.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.extractUsername(header.substring("Bearer ".length()));
    }
}
