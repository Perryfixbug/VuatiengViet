package vuatiengvietpj.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Session {
    private String sessionId;
    private long userId;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
    private String ipAddress;
    private String userAgent;
    private boolean isActive;
    private Long currentRoomId;

    public Session() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.isActive = true;
    }

    public Session(String sessionId, long userId, String username, String email) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Long getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(Long currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public void updateLastAccess() {
        this.lastAccessTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", isActive=" + isActive +
                ", currentRoomId=" + currentRoomId +
                ", lastAccess=" + lastAccessTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                '}';
    }
}