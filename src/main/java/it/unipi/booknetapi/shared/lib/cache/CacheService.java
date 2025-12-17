package it.unipi.booknetapi.shared.lib.cache;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.shared.lib.database.RedisManager;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class CacheService {

    private final RedisManager redisManager;
    private final ObjectMapper objectMapper;

    public CacheService(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.objectMapper = new ObjectMapper();
    }


    /**
     * Save any object to Redis as a JSON string.
     */
    public <T> void save(String key, T value) {
        // 1. Serialize Object -> JSON String
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }

        // 2. Save to Redis
        try (Jedis jedis = redisManager.getResource()) {
            jedis.set(key, jsonString);
        }
    }


    /**
     * Overload: Save with an expiration time (TTL) in seconds.
     */
    public <T> void save(String key, T value, int ttlSeconds) {
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }

        try (Jedis jedis = redisManager.getResource()) {
            jedis.setex(key, ttlSeconds, jsonString);
        }
    }


    /**
     * Retrieve an object from Redis and convert it back to the specific Class.
     * Returns null if the key doesn't exist.
     */
    public <T> T get(String key, Class<T> targetClass) {
        // 1. Get String from Redis
        String jsonString;
        try (Jedis jedis = redisManager.getResource()) {
            jsonString = jedis.get(key);
        }

        // 2. If null, return null immediately
        if (jsonString == null) {
            return null;
        }

        // 3. Deserialize JSON String -> Object
        try {
            return objectMapper.readValue(jsonString, targetClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON to object", e);
        }
    }


    /**
     * Delete a key
     */
    public void delete(String key) {
        try (Jedis jedis = redisManager.getResource()) {
            jedis.del(key);
        }
    }

}
