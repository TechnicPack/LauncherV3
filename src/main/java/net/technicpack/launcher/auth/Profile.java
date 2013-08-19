package net.technicpack.launcher.auth;

public class Profile {
	private String id;
	private String name;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Profile{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
