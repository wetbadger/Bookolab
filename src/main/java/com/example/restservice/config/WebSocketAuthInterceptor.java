package com.example.restservice.config;

import com.example.restservice.service.JwtService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            System.out.println("Received WebSocket Auth Header: " + authorizationHeader); // Debug log

            if (authorizationHeader != null) {
                String jwt = null;

                // Handle BOTH "Bearer <token>" and "Bearer<token>" safely
                if (authorizationHeader.startsWith("Bearer ")) {
                    jwt = authorizationHeader.substring(7).trim();
                } else if (authorizationHeader.startsWith("Bearer")) {
                    jwt = authorizationHeader.substring(6).trim();
                }

                if (jwt != null && !jwt.isEmpty()) {
                    try {
                        String username = jwtService.extractUsername(jwt);

                        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            try {
                                // Safe Database Lookup
                                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                                if (!userDetails.isEnabled()) {
                                    System.out.println("🚫 Connection rejected: User '" + username + "' is currently banned/disabled.");
                                    return null; // Returning null cancels the message and drops the STOMP connection frame
                                }

                                if (jwtService.isTokenValid(jwt, userDetails)) {
                                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    );
                                    accessor.setUser(authToken);
                                    System.out.println("Successfully authenticated WebSocket user: " + username);
                                }
                            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ue) {
                                // Catch the stale token scenario safely without crashing the execution channel
                                System.err.println("⚠️ Token is structurally valid, but user '" + username + "' no longer exists in the DB (Server reset?).");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("CRITICAL: JWT Processing failed in WebSocket Interceptor!");
                        e.printStackTrace();
                    }
                }
            }
        }
        return message;
    }
}