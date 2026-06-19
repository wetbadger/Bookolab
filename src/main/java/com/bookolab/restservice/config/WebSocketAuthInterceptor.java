package com.bookolab.restservice.config;

import com.bookolab.restservice.service.JwtService;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    // 🚀 Inject Redis-backed Bucket4j management components
    @Autowired
    private ProxyManager<byte[]> proxyManager;

    @Autowired
    private BucketConfiguration webSocketBucketConfiguration;

    // Use @Lazy to prevent circular dependency issues during startup
    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

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
                            setAnonymousUser(accessor);
                        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                            setAnonymousUser(accessor);
                        } catch (Exception e) {
                            e.printStackTrace();
                            setAnonymousUser(accessor);
                        }
                    }
                } else {
                    setAnonymousUser(accessor);
                }
            }

            // --- 2. HANDLE SEND COMMANDS (AUTH CHECK + RATE LIMITING) ---
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                // Auth Check: Ensure user is authenticated
                if (accessor.getUser() == null || accessor.getUser() instanceof AnonymousAuthenticationToken) {
                    System.err.println("🚫 Anonymous session blocked from sending.");
                    return null;
                }

                // Route Check: Only protect word sending
                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/app/send-word")) {

                    String username = accessor.getUser().getName();

                    if (username != null) {
                        // Key Strategy: Use a distinct prefix key string for Redis matching the user
                        byte[] redisKey = ("ratelimit:ws:" + username).getBytes();

                        // Replaces local cache lookup. Fetches or generates state remotely from Redis
                        BucketProxy bucket = proxyManager.builder().build(redisKey, webSocketBucketConfiguration);

                        if (!bucket.tryConsume(1)) {
                            System.out.println("🎰 " + username + " is typing too fast. Rate limit enforced via Redis.");

                            messagingTemplate.convertAndSendToUser(
                                    username,
                                    "/queue/errors",
                                    "Slow down! You are sending words too fast."
                            );

                            return null; // Silently drop the spam command execution
                        }
                    }
                }
            }

            // --- 3. CLEAN UP ON DISCONNECT ---
            // 🚀 Wiping data block completely removed!
            // Tokens persist securely inside Redis now, preserving limits across browser refreshes.
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