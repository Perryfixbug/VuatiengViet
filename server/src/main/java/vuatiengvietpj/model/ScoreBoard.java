package vuatiengvietpj.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScoreBoard implements Serializable {
    private Long roomId;
    private List<Player> Player = new ArrayList<>();
    private Instant updateAt;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public List<Player> getPlayer() {
        return Player;
    }

    public void setPlayer(List<Player> player) {
        Player = player;
    }

    public Instant getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Instant updateAt) {
        this.updateAt = updateAt;
    }

}
