package vuatiengvietpj.model;

public class Player {
	private Long userId;
	private String name;

	public Player() {
	}

	public Player(Long userId, String name) {
		this.userId = userId;
		this.name = name;
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

	@Override
	public String toString() {
		return "Player{" + "userId=" + userId + ", name='" + name + '\'' + '}';
	}

}
