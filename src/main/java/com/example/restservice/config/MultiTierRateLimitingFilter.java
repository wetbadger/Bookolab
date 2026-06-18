package com.example.restservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiTierRateLimitingFilter implements Filter {

    private final RateLimitConfig config;
    private final Bucket globalBucket;

    private final Map<String, Bucket> ipCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userCache = new ConcurrentHashMap<>();

    // 1. Inject the configuration class here
    @Autowired
    public MultiTierRateLimitingFilter(RateLimitConfig config) {
        this.config = config;

        // 2. Initialize the global bucket using the dynamic properties
        this.globalBucket = Bucket.builder()
                .addLimit(Bandwidth.classic(
                        config.getGlobalCapacity(),
                        Refill.greedy(config.getGlobalCapacity(), Duration.ofMinutes(1))
                ))
                .build();
    }

    // 3. Update the helper methods to use config properties
    private Bucket createIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        config.getIpCapacity(),
                        Refill.greedy(config.getIpCapacity(), Duration.ofMinutes(1))
                ))
                .build();
    }

    private Bucket createUserBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        config.getUserCapacity(),
                        Refill.greedy(config.getUserCapacity(), Duration.ofMinutes(1))
                ))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ipAddress = httpRequest.getRemoteAddr();
        String userId = httpRequest.getHeader("X-User-ID");

        Bucket ipBucket = ipCache.computeIfAbsent(ipAddress, k -> createIpBucket());
        Bucket userBucket = (userId != null) ? userCache.computeIfAbsent(userId, k -> createUserBucket()) : null;

        // Pipeline checks remain exactly the same...
        if (!globalBucket.tryConsume(1)) {
            sendThrottledResponse(httpResponse, "Server is under heavy load.");
            return;
        }

        if (!ipBucket.tryConsume(1)) {
            sendThrottledResponse(httpResponse, "Too many requests from your network.");
            return;
        }

        if (userBucket != null && !userBucket.tryConsume(1)) {
            sendThrottledResponse(httpResponse, "You have exceeded your account rate limit.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendThrottledResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("text/plain");
        response.getWriter().write(message);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {
        ipCache.clear();
        userCache.clear();
    }
}