package vuatiengvietpj.util;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RedisManager {
    private static JedisPool pool;
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    static {
        try {
            String host = ConfigManager.get("redis.host", "localhost");
            int port = ConfigManager.getInt("redis.port", 6379);
            int timeout = ConfigManager.getInt("redis.timeout", 2000);
            String password = ConfigManager.get("redis.password");
            int database = ConfigManager.getInt("redis.database", 0);

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(ConfigManager.getInt("redis.pool.maxTotal", 16));
            config.setMaxIdle(ConfigManager.getInt("redis.pool.maxIdle", 8));
            config.setMinIdle(ConfigManager.getInt("redis.pool.minIdle", 2));
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);

            if (password != null && !password.isEmpty()) {
                pool = new JedisPool(config, host, port, timeout, password, database);
            } else {
                pool = new JedisPool(config, host, port, timeout);
            }
            System.out.println("RedisManager: Pool initialized (host=" + host + ", port=" + port + ")");
        } catch (Exception e) {
            System.err.println("Redis connection failed during initialization: " + e.getMessage());
            System.err.println("⚠️  Redis is not available. Session management will be disabled.");
            pool = null;
        }
    }

    public static Jedis getResource() {
        if (pool == null) {
            return null;
        }
        try {
            return pool.getResource();
        } catch (Exception e) {
            System.err.println("Redis getResource error: " + e.getMessage());
            return null;
        }
    }

    public static void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    public static <T> void setCache(String key, T object, int ttlSeconds) {
        try (Jedis jedis = getResource()) {
            if (jedis != null) {
                String json = GSON.toJson(object);
                jedis.setex(key, ttlSeconds, json);
            }
        } catch (Exception e) {
            System.err.println("Redis set error: " + e.getMessage());
        }
    }

    public static <T> T getCache(String key, Class<T> clazz) {
        try (Jedis jedis = getResource()) {
            if (jedis != null) {
                String json = jedis.get(key);
                if (json != null && !json.isEmpty()) {
                    return GSON.fromJson(json, clazz);
                }
            }
        } catch (Exception e) {
            System.err.println("Redis get error: " + e.getMessage());
        }
        return null;
    }

    public static void deleteCache(String key) {
        try (Jedis jedis = getResource()) {
            if (jedis != null) {
                jedis.del(key);
            }
        } catch (Exception e) {
            System.err.println("Redis delete error: " + e.getMessage());
        }
    }
}