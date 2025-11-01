package vuatiengvietpj.model;

import java.io.Serializable;

public class Player implements Serializable {

    private Long id;
    private Long userId;
    private Long roomId;
    private int score;
    private String name; // Tên người chơi (fullName từ User)

    public Player() {

    }

    public Player(Long id, Long userId, Long roomId, int score) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
