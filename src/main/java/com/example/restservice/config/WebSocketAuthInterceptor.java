package com.example.restservice.config;

import com.example.restservice.service.JwtService;
import io.github.bucket4j.Bucket; // 🚀 Add Bucket4j imports
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException; // 🚀 For throwing errors
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    // 1. Inject your rate limit configuration 🚀
    @Autowired
    private RateLimitConfig rateLimitConfig;

    // Use @Lazy to prevent circular dependency issues during startup
    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

    // 2. Add your thread-safe session cache 🚀
    private final Map<String, Bucket> userWsCache = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            // --- 1. HANDLE CONNECTION INITIATION ---
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

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
                                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                                if (!userDetails.isEnabled()) {
                                    // Banned users get completely disconnected
                                    return null;
                                }

                                if (jwtService.isTokenValid(jwt, userDetails)) {
                                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    );
                                    accessor.setUser(authToken);
                                }
                            }
                        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ue) {
                            System.err.println("⚠️ Token valid, but user no longer exists in DB. Proceeding as Guest.");
                            setAnonymousUser(accessor);
                        } catch (ExpiredJwtException e) {
                            // System.err.println("⚠️ JWT has expired. Setting session to Read-Only Guest.");
                            setAnonymousUser(accessor);
                        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                            // System.err.println("⚠️ Invalid JWT structure/signature. Setting session to Read-Only Guest.");
                            setAnonymousUser(accessor);
                        } catch (Exception e) {
                            // System.err.println("CRITICAL: Unexpected JWT error in Interceptor. Setting session to Read-Only Guest.");
                            e.printStackTrace();
                            setAnonymousUser(accessor);
                        }
                    }
                } else {
                    // No header provided -> Fallback to Guest
                    setAnonymousUser(accessor);
                }
            }

            // --- 2. HANDLE SEND COMMANDS (AUTH CHECK + RATE LIMITING) ---
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                // 1. Auth Check: Ensure user is authenticated
                if (accessor.getUser() == null || accessor.getUser() instanceof AnonymousAuthenticationToken) {
                    System.err.println("🚫 Anonymous session blocked from sending.");
                    return null;
                }

                // 2. Route Check: Only protect word sending
                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/app/send-word")) {

                    // Pull the unique Username instead of the Session ID!
                    String username = accessor.getUser().getName();

                    if (username != null) {
                        // Check the cache using their unchangeable username
                        Bucket bucket = userWsCache.computeIfAbsent(username, k -> rateLimitConfig.createWebSocketBucket());

                        // System.out.println("🎰 User '" + username + "' has tokens remaining: " + bucket.getAvailableTokens());

                        if (!bucket.tryConsume(1)) {
                            // Send a targeted, private message to this specific user
                            messagingTemplate.convertAndSendToUser(
                                    username,
                                    "/queue/errors",
                                    "Slow down! You are sending messages too fast."
                            );

                            // Return null so the spammy message is quietly dropped,
                            // instead of throwing an exception that destroys the connection.
                            return null;
                        }
                    }
                }
            }

            // --- 3. CLEAN UP ON DISCONNECT ---
            if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    String username = accessor.getUser().getName();
                    if (username != null) {
                        userWsCache.remove(username);
                    }
                }
            }
        }
        return message;
    }

    private void setAnonymousUser(StompHeaderAccessor accessor) {
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                "anonymousKey", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        accessor.setUser(anonymousToken);
    }
}