package vuatiengvietpj.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {
    private Long id;
    private Long ownerId;
    private int maxPlayer;
    private List<Player> players = new ArrayList<>();
    private Instant createAt;
    private ChallengePack cp;
    private String status;

    public Room() {

    }

    public Room(Long id, Long ownerId, int maxPlayer, List<Player> players, Instant createAt, ChallengePack cp,
            String status) {
        this.id = id;
        this.ownerId = ownerId;
        this.maxPlayer = maxPlayer;
        this.players = players;
        this.createAt = createAt;
        this.cp = cp;
        this.status = status;
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

    @Override
    public String toString() {
        return "Room [id=" + id + ", ownerId=" + ownerId + ", maxPlayer=" + maxPlayer + ", players=" + players
                + ", createAt=" + createAt + ", cp=" + cp + ", status=" + status + "]";
    }

}
