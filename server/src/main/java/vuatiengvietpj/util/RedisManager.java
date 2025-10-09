package vuatiengvietpj.util;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import java.time.Duration;

public class RedisManager {
    private static JedisPool pool;
    private static final String HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    private static final int TIMEOUT_MS = Integer.parseInt(System.getenv().getOrDefault("REDIS_TIMEOUT_MS", "2000"));

    static {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(16);
        cfg.setMaxIdle(8);
        cfg.setMinIdle(1);
        pool = new JedisPool(cfg, HOST, PORT, TIMEOUT_MS);
    }

    public static Jedis getResource() {
        return pool.getResource();
    }

    public static void close() {
        if (pool != null)
            pool.close();
    }
}