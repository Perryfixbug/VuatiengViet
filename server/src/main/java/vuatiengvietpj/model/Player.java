package vuatiengvietpj.model;

import java.io.Serializable;

public class Player implements Serializable {

    private Long id;
    private User user;
    private Long roomId;
    private int score;

    public Player() {

    }

    public Player(Long id, User user, Long roomId, int score) {
        this.id = id;
        this.user = user;
        this.roomId = roomId;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

}
