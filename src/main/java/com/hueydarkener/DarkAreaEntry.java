package com.hueydarkener;

import java.util.ArrayList;
import java.util.List;

final class DarkAreaEntry
{
	private String name;
	private List<Integer> regionIds;
	private int darkness;
	private boolean enabled;

	DarkAreaEntry()
	{
		this("", new ArrayList<>(), 45, true);
	}

	DarkAreaEntry(String name, List<Integer> regionIds, int darkness, boolean enabled)
	{
		this.name = name;
		this.regionIds = new ArrayList<>(regionIds);
		this.darkness = darkness;
		this.enabled = enabled;
	}

	String getName()
	{
		return name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	List<Integer> getRegionIds()
	{
		return regionIds;
	}

	void setRegionIds(List<Integer> regionIds)
	{
		this.regionIds = new ArrayList<>(regionIds);
	}

	int getDarkness()
	{
		return darkness;
	}

	void setDarkness(int darkness)
	{
		this.darkness = darkness;
	}

	boolean isEnabled()
	{
		return enabled;
	}

	void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	boolean containsRegion(int regionId)
	{
		return regionIds.contains(regionId);
	}
}
