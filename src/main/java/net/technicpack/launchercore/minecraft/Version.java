package net.technicpack.launchercore.minecraft;

import java.util.Date;

public interface Version {

	public String getId();

	public ReleaseType getType();

	public void setType(ReleaseType releaseType);

	public Date getUpdatedTime();

	public void setUpdatedTime(Date updatedTime);

	public Date getReleaseTime();

	public void setReleaseTime(Date releaseTime);
}
