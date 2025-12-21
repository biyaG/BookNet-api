package it.unipi.booknetapi.shared.lib.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheService {

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    public CacheService(JedisPool jedisPool, MeterRegistry registry) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        this.registry = registry;
    }

    private <T> String toJson(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON Error", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON Error", e);
        }
    }

    /**
     * Save any object to Redis as a JSON string.
     */
    public <T> void save(String key, T value) {
        registry.timer("redis.ops", "cmd", "set").record(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(key, toJson(value));
            }
        });
    }


    /**
     * Overload: Save with an expiration time (TTL) in seconds.
     */
    public <T> void save(String key, T value, int ttlSeconds) {
        registry.timer("redis.ops", "cmd", "set").record(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(key, ttlSeconds, toJson(value));
            }
        });
    }


    /**
     * Retrieve an object from Redis and convert it back to the specific Class.
     * Returns null if the key doesn't exist.
     */
    public <T> T get(String key, Class<T> targetClass) {
        return registry.timer("redis.ops", "cmd", "get").record(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String json = jedis.get(key);
                return (json == null) ? null : fromJson(json, targetClass);
            }
        });
    }


    /**
     * Delete a key
     */
    public void delete(String key) {
        registry.timer("redis.ops", "cmd", "delete").record(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(key);
            }
        });
    }

}
