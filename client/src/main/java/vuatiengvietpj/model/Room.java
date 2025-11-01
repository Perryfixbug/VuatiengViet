package vuatiengvietpj.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Room {
    private Long id;
    private Long ownerId;
    private String ownerName; // Tên chủ phòng
    private int maxPlayer;
    private List<Player> players = new ArrayList<>();
    private Instant createAt;
    private String status;

    public Room() {
    }

    public Room(Long id, Long ownerId, int maxPlayer, Instant createAt, String status, List<Player> players) {
        this.id = id;
        this.ownerId = ownerId;
        this.maxPlayer = maxPlayer;
        this.createAt = createAt;
        this.status = status;
        this.players = players;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public void setMaxPlayer(int maxPlayer) {
        this.maxPlayer = maxPlayer;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public Instant getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Instant createAt) {
        this.createAt = createAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public String toString() {
        return "Room [id=" + id + ", ownerId=" + ownerId + ", ownerName=" + ownerName + ", maxPlayer=" + maxPlayer + ", players=" + players
                + ", createAt=" + createAt + ", status=" + status + "]";
    }
}
