package it.unipi.booknetapi.shared.lib.database;

import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import jakarta.annotation.PreDestroy;

@Component
public class RedisManager {

    private final JedisPool jedisPool;

    // 1. Initialize the Pool ONCE
    public RedisManager(AppConfig config) {
        System.out.println("Initializing Redis Pool...");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128); // Max active connections
        poolConfig.setMaxIdle(16);

        // Handle password logic (Redis expects null if no password, not empty string)
        String password = (config.getRedisPassword() == null || config.getRedisPassword().isEmpty())
                ? null
                : config.getRedisPassword();

        this.jedisPool = new JedisPool(
                poolConfig,
                config.getRedisHost(),
                config.getRedisPort(),
                config.getRedisTimeout(),
                password
        );
    }

    public Jedis getResource() {
        return jedisPool.getResource();
    }

    // 3. Cleanup on App Shutdown
    @PreDestroy
    public void close() {
        System.out.println("Closing Redis Pool...");
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

}
