package vuatiengvietpj.model;

import java.io.Serializable;

public class Player implements Serializable {

    private Integer id;
    private Integer userId;
    private Integer roomId;
    private Integer score;
    private String name; // Tên người chơi (fullName từ User)

    public Player() {

    }

    public Player(Integer id, Integer userId, Integer roomId, Integer score) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.score = score;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
