package vuatiengvietpj.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import vuatiengvietpj.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SessionManager {
    private static final String KEY_PREFIX = "user:session:";
    private static final int SESSION_TTL = ConfigManager.getInt("session.ttl", 3600); // seconds
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation() // bỏ qua createAt/updateAt nếu không @Expose
            .create();

    private static String key(Integer userId) {
        return KEY_PREFIX + userId;
    }

    public static boolean createOrUpdate(User user, String ip) {
        if (user == null || user.getId() == null)
            return false;

        String userJson = GSON.toJson(user);

        String sessionId = UUID.randomUUID().toString();// gen sessionId ngãu nhiên

        Map<String, String> data = new HashMap<>();
        data.put("user", userJson);
        data.put("loginAtMs", String.valueOf(System.currentTimeMillis()));
        data.put("ip", ip);
        data.put("sessionId", sessionId);
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null) {
                System.err.println("SessionManager: Redis is not available, session will not be stored");
                return false;
            }
            jedis.hset(key(user.getId()), data);
            jedis.expire(key(user.getId()), SESSION_TTL);
            return true;
        } catch (Exception e) {
            System.err.println("SessionManager.createOrUpdate error: " + e.getMessage());
            return false;
        }

    }

    public static String getSessionId(Integer userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null) {
                System.out.println("Chua ket noi Redis");
                return "ko co session";
            }
            return jedis.hget(key(userId), "sessionId");
        } catch (Exception e) {
            System.err.println("Loi lay sessionId: " + e.getMessage());
            return "ko co session";
        }
    }

    public static boolean checkSessionId(String needCheckSesssionId, Integer userId) {
        if (needCheckSesssionId.equals("OK"))
            return true;
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null) {
                System.out.println("Chua ket noi Redis");
                return false;
            }
            String sessionId = jedis.hget(key(userId), "sessionId");
            if (sessionId == null || !needCheckSesssionId.equals(sessionId)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi check sessionId: " + e.getMessage());
            return false;
        }
    }

    public static User getUser(Integer userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return null;
            String json = jedis.hget(key(userId), "user");
            return (json == null || json.isEmpty()) ? null : GSON.fromJson(json, User.class);
        } catch (Exception e) {
            System.err.println("SessionManager.getUser error: " + e.getMessage());
            return null;
        }
    }

    public static Integer getLoginAtMs(Integer userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return null;
            String v = jedis.hget(key(userId), "loginAtMs");
            return (v == null) ? null : Integer.parseInt(v);
        } catch (Exception e) {
            System.err.println("SessionManager.getLoginAtMs error: " + e.getMessage());
            return null;
        }
    }

    public static boolean checkNewIpAddressforSession(Integer userId, String newIp) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return false;
            String oldIp = jedis.hget(key(userId), "ip");
            if (oldIp == null || !oldIp.equals(newIp)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("SessionManager.checkNewIpAddressforSession error: " + e.getMessage());
            return false;
        }
    }

    public static boolean isLoggedIn(Integer userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            return jedis != null && jedis.exists(key(userId));
        } catch (Exception e) {
            System.err.println("SessionManager.isLoggedIn error: " + e.getMessage());
            return false;
        }
    }

    public static boolean destroy(Integer userId) {
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
            return del > 0;
        } catch (Exception e) {
            System.err.println("SessionManager.destroy error: " + e.getMessage());
            return false;
        }
    }

    public static List<User> getOnlineUsers() {
        List<User> users = new ArrayList<>();
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis == null)
                return users;
            Set<String> keys = jedis.keys(KEY_PREFIX + "*");
            for (String key : keys) {
                String json = jedis.hget(key, "user");
                if (json != null && !json.isEmpty()) {
                    User u = GSON.fromJson(json, User.class);
                    if (u != null) {
                        users.add(u);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SessionManager.getOnlineUsers error: " + e.getMessage());
        }
        return users;
    }
}