package vuatiengvietpj.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Room implements Serializable {
    @Expose
    private Long id;
    @Expose
    private Long ownerId;
    @Expose
    private String ownerName; // Tên chủ phòng
    @Expose
    private int maxPlayer;
    @Expose
    private List<Player> players = new ArrayList<>();
    @Expose
    private Instant createAt;
    @Expose
    private ChallengePack cp;
    @Expose
    private String status;

    public Room() {

    }

    public Room(Long id, Long ownerId, int maxPlayer, Instant createAt, String status, ChallengePack cp, List<Player> players) {
        this.id = id;
        this.ownerId = ownerId;
        this.maxPlayer = maxPlayer;
        this.createAt = createAt;
        this.status = status;
        this.cp = cp;
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

    public ChallengePack getCp() {
        return cp;
    }

    public void setCp(ChallengePack cp) {
        this.cp = cp;
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
                + ", createAt=" + createAt + ", cp=" + cp + ", status=" + status + "]";
    }
    
}
