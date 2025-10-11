package vuatiengvietpj.util;

import vuatiengvietpj.model.Session;

public class SessionFilter {

    /**
     * Kiểm tra authentication
     */
    public static boolean requireAuth(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        Session session = SessionManager.getSession(sessionId);
        return session != null && session.isActive();
    }

    /**
     * Kiểm tra user có trong room không
     */
    public static boolean requireInRoom(String sessionId, long roomId) {
        Session session = SessionManager.getSession(sessionId);
        if (session == null || !session.isActive()) {
            return false;
        }

        Long currentRoomId = session.getCurrentRoomId();
        return currentRoomId != null && currentRoomId == roomId;
    }

    /**
     * Lấy user ID từ session
     */
    public static Long getUserId(String sessionId) {
        Session session = SessionManager.getSession(sessionId);
        return session != null ? session.getUserId() : null;
    }

    /**
     * Lấy thông tin user từ session
     */
    public static Session getUserSession(String sessionId) {
        return SessionManager.getSession(sessionId);
    }
}