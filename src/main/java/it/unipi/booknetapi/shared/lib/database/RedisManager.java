package it.unipi.booknetapi.shared.lib.database;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.commonspool2.CommonsObjectPool2Metrics;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

@Configuration
public class RedisManager {

    private final AppConfig appConfig;

    // 1. Initialize the Pool ONCE
    public RedisManager(AppConfig config) {
        // System.out.println("Initializing Redis Pool...");

        /*JedisPoolConfig poolConfig = new JedisPoolConfig();
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
        );*/

        this.appConfig = config;
    }


    @Bean(destroyMethod = "close") // Spring will automatically call .close() on shutdown
    public JedisPool jedisPool(MeterRegistry registry) {
        // System.out.println("Initializing Redis Pool via @Bean...");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(16);

        poolConfig.setJmxEnabled(false);

        String password = (appConfig.getRedisPassword() == null || appConfig.getRedisPassword().isEmpty())
                ? null
                : appConfig.getRedisPassword();

        JedisPool pool = new JedisPool(
                poolConfig,
                appConfig.getRedisHost(),
                appConfig.getRedisPort(),
                appConfig.getRedisTimeout(),
                password
        );

        // --- MANUAL METRICS BINDING ---
        // This tells Prometheus to track "Active Connections" and "Idle Connections"
        // for this specific pool.
        /*new CommonsObjectPool2Metrics(pool, "redis", Collections.singletonList(Tag.of("name", "main-pool")))
                .bindTo(registry);*/

        // --- MANUAL METRICS REGISTRATION ---

        // 1. Monitor Active Connections
        Gauge.builder("redis.pool.active", pool, JedisPool::getNumActive)
                .description("The number of active connections currently borrowed from the pool")
                .register(registry);

        // 2. Monitor Idle Connections
        Gauge.builder("redis.pool.idle", pool, JedisPool::getNumIdle)
                .description("The number of idle connections in the pool")
                .register(registry);

        return pool;
    }

}
