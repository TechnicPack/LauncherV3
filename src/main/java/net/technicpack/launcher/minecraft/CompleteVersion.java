package net.technicpack.launcher.minecraft;

import java.util.Date;
import java.util.List;

public class CompleteVersion implements Version {

	private String id;
	private Date time;
	private Date releaseTime;
	private ReleaseType type;
	private String minecraftArguments;
	private List<Library> libraries;
	private String mainClass;
	private int minimumLauncherVersion;
	private String incompatibilityReason;
	private List<Rule> rules;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ReleaseType getType() {
		return type;
	}

	@Override
	public void setType(ReleaseType type) {
		this.type = type;
	}

	@Override
	public Date getUpdatedTime() {
		return time;
	}

	@Override
	public void setUpdatedTime(Date updatedTime) {
		this.time = updatedTime;
	}

	@Override
	public Date getReleaseTime() {
		return releaseTime;
	}

	@Override
	public void setReleaseTime(Date releaseTime) {
		this.releaseTime = releaseTime;
	}

	public String getMinecraftArguments() {
		return minecraftArguments;
	}

	public List<Library> getLibraries() {
		return libraries;
	}

	public String getMainClass() {
		return mainClass;
	}

	public int getMinimumLauncherVersion() {
		return minimumLauncherVersion;
	}

	public String getIncompatibilityReason() {
		return incompatibilityReason;
	}

	public List<Rule> getRules() {
		return rules;
	}
}
