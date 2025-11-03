package vuatiengvietpj.model;

public class Dictionary {
    private String word;
    private Long frequency;

    public Dictionary() {
    }

    public Dictionary(String word, String meaning, Long frequency) {
        this.word = word;
        this.frequency = frequency;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Long getFrequency() {
        return frequency;
    }

    public void setFrequency(Long frequency) {
        this.frequency = frequency;
    }

}
