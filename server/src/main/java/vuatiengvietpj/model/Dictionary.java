package vuatiengvietpj.model;

public class Dictionary {
    private String word;
    private String meaning;
    private Long frequency;

    public Dictionary() {
    }

    public Dictionary(String word, String meaning, Long frequency) {
        this.word = word;
        this.meaning = meaning;
        this.frequency = frequency;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public Long getFrequency() {
        return frequency;
    }

    public void setFrequency(Long frequency) {
        this.frequency = frequency;
    }

}
