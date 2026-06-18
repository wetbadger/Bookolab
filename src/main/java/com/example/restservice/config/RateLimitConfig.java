package com.example.restservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${ratelimit.global.capacity}")
    private int globalCapacity;

    @Value("${ratelimit.ip.capacity}")
    private int ipCapacity;

    @Value("${ratelimit.user.capacity}")
    private int userCapacity;

    // 1. Read the new WebSocket property
    @Value("${ratelimit.websocket.capacity}")
    private int webSocketCapacity;

    // Getters
    public int getGlobalCapacity() { return globalCapacity; }
    public int getIpCapacity() { return ipCapacity; }
    public int getUserCapacity() { return userCapacity; }
    public int getWebSocketCapacity() { return webSocketCapacity; }

    // 2. Add the factory method your Interceptor is trying to call 🚀
    public Bucket createWebSocketBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        webSocketCapacity,
                        Refill.greedy(webSocketCapacity, Duration.ofMinutes(1))
                ))
                .build();
    }
}