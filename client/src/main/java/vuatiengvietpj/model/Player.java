package vuatiengvietpj.model;

public class Player {
	private Long userId;
	private String name;
	private int score;

	public Player() {
	}

	public Player(Long userId, String name) {
		this.userId = userId;
		this.name = name;
		this.score = 0;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "Player{" + "userId=" + userId + ", name='" + name + '\'' + ", score=" + score + '}';
	}

}
