package vuatiengvietpj.model;

public class Player {
	private Integer userId;
	private String name;
	private Integer score;

	public Player() {
	}

	public Player(Integer userId, String name) {
		this.userId = userId;
		this.name = name;
		this.score = 0;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "Player{" + "userId=" + userId + ", name='" + name + '\'' + ", score=" + score + '}';
	}

}
