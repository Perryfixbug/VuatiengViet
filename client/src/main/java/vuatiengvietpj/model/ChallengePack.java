
package vuatiengvietpj.model;

import java.io.Serializable;
import java.util.Arrays;

public class ChallengePack implements Serializable {
    private Integer id;
    private char[] quizz;
    private Integer level;

    public ChallengePack() {

    }

    public ChallengePack(Integer id, char[] quizz, Integer level) {
        this.id = id;
        this.quizz = quizz;
        this.level = level;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public char[] getQuizz() {
        return quizz;
    }

    public void setQuizz(char[] quizz) {
        this.quizz = quizz;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "ChallengePack [id=" + id + ", quizz=" + Arrays.toString(quizz) + ", level=" + level + "]";
    }

}