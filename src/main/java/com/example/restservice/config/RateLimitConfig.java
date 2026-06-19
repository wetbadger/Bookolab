package com.example.restservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // 🚀 Inject host and port directly (fallback to localhost:6379 if empty)
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${ratelimit.global.capacity}")
    private int globalCapacity;

    @Value("${ratelimit.ip.capacity}")
    private int ipCapacity;

    @Value("${ratelimit.user.capacity}")
    private int userCapacity;

    @Value("${ratelimit.websocket.capacity}")
    private int webSocketCapacity;

    public int getGlobalCapacity() { return globalCapacity; }
    public int getIpCapacity() { return ipCapacity; }
    public int getUserCapacity() { return userCapacity; }
    public int getWebSocketCapacity() { return webSocketCapacity; }

    /**
     * Manually expose the native RedisClient Bean that Bucket4j needs.
     */
    @Bean
    public RedisClient redisClient() {
        RedisURI redisUri = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .build();
        return RedisClient.create(redisUri);
    }

    @Bean
    public BucketConfiguration webSocketBucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        webSocketCapacity,
                        Refill.greedy(webSocketCapacity, Duration.ofMinutes(1))
                ))
                .build();
    }

    /**
     * The Redis ProxyManager can naturally resolve custom RedisClient bean above.
     */
    @Bean
    public ProxyManager<byte[]> lettuceProxyManager(RedisClient redisClient) {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        return LettuceBasedProxyManager.builderFor(connection).build();
    }
}