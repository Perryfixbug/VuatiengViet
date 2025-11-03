package vuatiengvietpj.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Room {
    private Integer id;
    private Integer ownerId;
    private String ownerName; // Tên chủ phòng
    private Integer maxPlayer;
    private List<Player> players = new ArrayList<>();
    private Instant createAt;
    private ChallengePack cp; // Challenge pack được gán cho room
    private String status;

    public Room() {
    }

    public Room(Integer id, Integer ownerId, Integer maxPlayer, Instant createAt, String status, List<Player> players) {
        this.id = id;
        this.ownerId = ownerId;
        this.maxPlayer = maxPlayer;
        this.createAt = createAt;
        this.status = status;
        this.players = players;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getMaxPlayer() {
        return maxPlayer;
    }

    public void setMaxPlayer(Integer maxPlayer) {
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

    public ChallengePack getCp() {
        return cp;
    }

    public void setCp(ChallengePack cp) {
        this.cp = cp;
    }

    @Override
    public String toString() {
        return "Room [id=" + id + ", ownerId=" + ownerId + ", ownerName=" + ownerName + ", maxPlayer=" + maxPlayer + ", players=" + players
                + ", createAt=" + createAt + ", cp=" + cp + ", status=" + status + "]";
    }
}
