package com.example.restservice.config;

import com.example.restservice.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
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
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            // 1. HANDLE CONNECTION INITIATION (ALLOW ANONYMOUS/GUEST FALLBACK)
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                System.out.println("Received WebSocket Auth Header: " + authorizationHeader);

                if (authorizationHeader != null) {
                    String jwt = null;

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
                                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                                    if (!userDetails.isEnabled()) {
                                        System.out.println("🚫 Connection rejected: User '" + username + "' is currently banned/disabled.");
                                        return null; // Banned users get completely disconnected
                                    }

                                    if (jwtService.isTokenValid(jwt, userDetails)) {
                                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities()
                                        );
                                        accessor.setUser(authToken);
                                        System.out.println("Successfully authenticated WebSocket user: " + username);
                                    }
                                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ue) {
                                    System.err.println("⚠️ Token valid, but user '" + username + "' no longer exists in DB. Proceeding as Guest.");
                                }
                            }
                        } catch (ExpiredJwtException e) {
                            System.err.println("⚠️ JWT has expired. Setting session to Read-Only Guest.");
                            setAnonymousUser(accessor);

                        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                            System.err.println("⚠️ Invalid JWT structure/signature. Setting session to Read-Only Guest.");
                            setAnonymousUser(accessor);

                        } catch (Exception e) {
                            System.err.println("CRITICAL: Unexpected JWT error in Interceptor. Setting session to Read-Only Guest.");
                            e.printStackTrace();
                            setAnonymousUser(accessor);
                        }
                    }
                } else {
                    System.out.println("-> Anonymous user browsing page. Proceeding as Guest.");
                }
            }

            // 2. BLOCK SEND COMMANDS FOR ANONYMOUS/GUEST USERS
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                if (accessor.getUser() == null ||
                        accessor.getUser() instanceof AnonymousAuthenticationToken) {

                    System.err.println("🚫 Read-Only Mode Block: Anonymous/Expired sessions cannot send messages.");
                    return null; // Drops the payload completely
                }
            }
        }
        return message;
    }
    private void setAnonymousUser(StompHeaderAccessor accessor) {
        // Creates a standard Spring Security Anonymous token
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                "anonymousKey", // unique key identifying the anonymous auth context
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );

        // Explicitly bind it to the STOMP session frame
        accessor.setUser(anonymousToken);
    }
}