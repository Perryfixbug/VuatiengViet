
package vuatiengvietpj.model;

import java.io.Serializable;
import java.util.Arrays;

public class ChallengePack implements Serializable {
    private long id;
    private char[] quizz;
    private int level;

    public ChallengePack() {

    }

    public ChallengePack(long id, char[] quizz, int level) {
        this.id = id;
        this.quizz = quizz;
        this.level = level;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public char[] getQuizz() {
        return quizz;
    }

    public void setQuizz(char[] quizz) {
        this.quizz = quizz;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "ChallengePack [id=" + id + ", quizz=" + Arrays.toString(quizz) + ", level=" + level + "]";
    }

}