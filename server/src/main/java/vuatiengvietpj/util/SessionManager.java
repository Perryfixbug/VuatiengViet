package vuatiengvietpj.util;

import vuatiengvietpj.model.Session;
import vuatiengvietpj.model.User;
import redis.clients.jedis.Jedis;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionManager {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> context
                            .serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                            .parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    private static final int SESSION_TTL = ConfigManager.getInt("session.ttl", 3600); // 1 hour default
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_session:";
    private static final String ACTIVE_SESSIONS_KEY = "active_sessions";

    /**
     * Tạo session mới cho user
     */
    public static Session createSession(User user, String ipAddress, String userAgent) {
        if (user == null || user.getId() <= 0)
            return null;

        // Tạo session ID unique
        String sessionId = generateSessionId();

        Session session = new Session(sessionId, user.getId(), user.getFullName(), user.getEmail());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);

        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                String sessionKey = SESSION_PREFIX + sessionId;
                String userSessionKey = USER_SESSION_PREFIX + user.getId();

                // Lưu session data
                String sessionJson = GSON.toJson(session);
                jedis.setex(sessionKey, SESSION_TTL, sessionJson);

                // Map user ID -> session ID
                jedis.setex(userSessionKey, SESSION_TTL, sessionId);

                // Thêm vào danh sách active sessions
                jedis.sadd(ACTIVE_SESSIONS_KEY, sessionId);

                return session;
            }
        } catch (Exception e) {
            System.err.println("Error creating session: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy session theo session ID
     */
    public static Session getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            return null;

        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                String sessionKey = SESSION_PREFIX + sessionId;
                String sessionJson = jedis.get(sessionKey);

                if (sessionJson != null && !sessionJson.isEmpty()) {
                    Session session = GSON.fromJson(sessionJson, Session.class);

                    // Update last access time
                    session.updateLastAccess();

                    // Extend TTL
                    jedis.setex(sessionKey, SESSION_TTL, GSON.toJson(session));

                    return session;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting session: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy session theo user ID
     */
    public static Session getSessionByUserId(long userId) {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                String userSessionKey = USER_SESSION_PREFIX + userId;
                String sessionId = jedis.get(userSessionKey);

                if (sessionId != null && !sessionId.isEmpty()) {
                    return getSession(sessionId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting session by user ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật session
     */
    public static boolean updateSession(Session session) {
        if (session == null || session.getSessionId() == null)
            return false;

        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                String sessionKey = SESSION_PREFIX + session.getSessionId();
                session.updateLastAccess();

                String sessionJson = GSON.toJson(session);
                jedis.setex(sessionKey, SESSION_TTL, sessionJson);

                return true;
            }
        } catch (Exception e) {
            System.err.println("Error updating session: " + e.getMessage());
        }
        return false;
    }

    /**
     * Xóa session (logout)
     */
    public static boolean destroySession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            return false;

        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                // Lấy session để biết user ID
                Session session = getSession(sessionId);
                if (session != null) {
                    String userSessionKey = USER_SESSION_PREFIX + session.getUserId();
                    jedis.del(userSessionKey);
                }

                // Xóa session
                String sessionKey = SESSION_PREFIX + sessionId;
                jedis.del(sessionKey);

                // Xóa khỏi active sessions
                jedis.srem(ACTIVE_SESSIONS_KEY, sessionId);

                return true;
            }
        } catch (Exception e) {
            System.err.println("Error destroying session: " + e.getMessage());
        }
        return false;
    }

    /**
     * Xóa tất cả session của user
     */
    public static boolean destroyUserSessions(long userId) {
        Session session = getSessionByUserId(userId);
        if (session != null) {
            return destroySession(session.getSessionId());
        }
        return false;
    }

    /**
     * Kiểm tra session có hợp lệ không
     */
    public static boolean isValidSession(String sessionId) {
        Session session = getSession(sessionId);
        return session != null && session.isActive();
    }

    /**
     * Lấy danh sách all active sessions (admin)
     */
    public static List<Session> getActiveSessions() {
        List<Session> sessions = new ArrayList<>();

        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                // Lấy tất cả session IDs
                for (String sessionId : jedis.smembers(ACTIVE_SESSIONS_KEY)) {
                    Session session = getSession(sessionId);
                    if (session != null && session.isActive()) {
                        sessions.add(session);
                    } else {
                        // Cleanup expired sessions
                        jedis.srem(ACTIVE_SESSIONS_KEY, sessionId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting active sessions: " + e.getMessage());
        }
        return sessions;
    }

    /**
     * Đếm số user online
     */
    public static long getOnlineUserCount() {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                return jedis.scard(ACTIVE_SESSIONS_KEY);
            }
        } catch (Exception e) {
            System.err.println("Error counting online users: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Update room ID cho session
     */
    public static boolean updateSessionRoom(String sessionId, Long roomId) {
        Session session = getSession(sessionId);
        if (session != null) {
            session.setCurrentRoomId(roomId);
            return updateSession(session);
        }
        return false;
    }

    /**
     * Cleanup expired sessions
     */
    public static void cleanupExpiredSessions() {
        try (Jedis jedis = RedisManager.getResource()) {
            if (jedis != null) {
                List<String> expiredSessions = new ArrayList<>();

                for (String sessionId : jedis.smembers(ACTIVE_SESSIONS_KEY)) {
                    String sessionKey = SESSION_PREFIX + sessionId;
                    if (!jedis.exists(sessionKey)) {
                        expiredSessions.add(sessionId);
                    }
                }

                // Remove expired sessions from active list
                for (String sessionId : expiredSessions) {
                    jedis.srem(ACTIVE_SESSIONS_KEY, sessionId);
                }

                System.out.println("Cleaned up " + expiredSessions.size() + " expired sessions");
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up sessions: " + e.getMessage());
        }
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis();
    }
}