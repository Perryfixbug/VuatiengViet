package vuatiengvietpj.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import vuatiengvietpj.model.User;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static final String KEY_PREFIX = "user:session:";
    public static final String CH_EVENTS = "session:events"; // ✅ kênh log
    private static final int SESSION_TTL = ConfigManager.getInt("session.ttl", 3600); // seconds
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation() // bỏ qua createAt/updateAt nếu không @Expose
            .create();

    private static String key(long userId) {
        return KEY_PREFIX + userId;
    }

    public static boolean createOrUpdate(User user, String ip) {
        if (user == null || user.getId() == null)
            return false;

        String userJson = GSON.toJson(user);
        Map<String, String> data = new HashMap<>();
        data.put("user", userJson);
        data.put("loginAtMs", String.valueOf(System.currentTimeMillis()));
        data.put("ip", ip);
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return false;
            jedis.hset(key(user.getId()), data);
            jedis.expire(key(user.getId()), SESSION_TTL);

            // ✅ Publish sự kiện LOGIN
            Map<String, Object> evt = new HashMap<>();
            evt.put("type", "LOGIN");
            evt.put("userId", user.getId());
            evt.put("email", user.getEmail());
            evt.put("loginAtMs", System.currentTimeMillis());
            evt.put("ip", ip);
            jedis.publish(CH_EVENTS, GSON.toJson(evt));
            return true;
        }

    }

    public static User getUser(long userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return null;
            String json = jedis.hget(key(userId), "user");
            return (json == null || json.isEmpty()) ? null : GSON.fromJson(json, User.class);
        }
    }

    public static Long getLoginAtMs(long userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return null;
            String v = jedis.hget(key(userId), "loginAtMs");
            return (v == null) ? null : Long.parseLong(v);
        }
    }

    public static boolean checkNewIpAddressforSession(long userId, String newIp) {
        Jedis jedis = RedisManager.getResource();
        if (jedis == null)
            return false;
        String oldIp = jedis.hget(key(userId), newIp);
        if (!oldIp.equals(newIp)) {
            return true;
        }
        return false;

    }

    public static boolean isLoggedIn(long userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            return jedis != null && jedis.exists(key(userId));
        }
    }

    public static boolean destroy(long userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return false;

            String k = key(userId);
            String json = jedis.hget(k, "user");
            String ip = jedis.hget(k, "ip");
            String email = null;
            if (json != null && !json.isEmpty()) {
                User u = GSON.fromJson(json, User.class);
                if (u != null)
                    email = u.getEmail();
            }
            long del = jedis.del(k);

            // ✅ Publish sự kiện LOGOUT
            Map<String, Object> evt = new HashMap<>();
            evt.put("type", "LOGOUT");
            evt.put("userId", userId);
            evt.put("email", email);
            evt.put("ip", ip);
            evt.put("logoutAtMs", System.currentTimeMillis());
            jedis.publish(CH_EVENTS, GSON.toJson(evt));

            return del > 0;
        }
    }
}
