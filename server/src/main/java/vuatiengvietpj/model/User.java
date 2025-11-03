package vuatiengvietpj.model;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.time.Instant;

import com.google.gson.annotations.Expose;

public class User implements Serializable {
    @Expose
    private Integer id;
    @Expose
    private String fullName;
    @Expose
    private String email;
    @Expose
    private String password;
    private Instant createAt;
    private Instant updateAt;
    @Expose
    private Integer totalScore;
    private static final long serialVersionUID = 1L;

    public User() {
    }

    public User(Integer id, String fullName, String email, String password, Instant createAt, Instant updateAt,
            Integer totalScore) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.createAt = createAt;
        this.updateAt = updateAt;
        this.totalScore = totalScore;
    }

    public String getFullName() {
        return fullName;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Instant getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Instant createAt) {
        this.createAt = createAt;
    }

    public Instant getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Instant updateAt) {
        this.updateAt = updateAt;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + ", password=" + password
                + ", createAt=" + createAt + ", updateAt=" + updateAt + ", totalScore=" + totalScore + "]";
    }

    public Integer getId() {
        return id;
    }

}