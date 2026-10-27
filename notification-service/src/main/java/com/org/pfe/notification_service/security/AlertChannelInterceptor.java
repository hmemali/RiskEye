package com.org.pfe.notification_service.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class AlertChannelInterceptor implements ChannelInterceptor {

    private static final String ALERT_DESTINATION = "/user/queue/alerts";
    private final NotificationJwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new MessageDeliveryException("A valid bearer token is required");
            }

            try {
                Claims claims = jwtService.parse(authorization.substring(7));
                String email = claims.getSubject();
                Number userId = claims.get("userId", Number.class);
                String role = claims.get("role", String.class);
                if (email == null || email.isBlank() || userId == null || role == null) {
                    throw new MessageDeliveryException("The bearer token has no user identity");
                }
                accessor.setUser(new NotificationPrincipal(userId.longValue(), email, role));
            } catch (MessageDeliveryException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("The bearer token is invalid", exception);
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null || !ALERT_DESTINATION.equals(accessor.getDestination())) {
                throw new MessageDeliveryException("Subscription is not authorized");
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new MessageDeliveryException("Client messages are not accepted");
        }

        return message;
    }
}