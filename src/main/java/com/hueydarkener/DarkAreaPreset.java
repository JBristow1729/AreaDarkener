package com.hueydarkener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class DarkAreaPreset
{
	private static final List<DarkAreaPreset> PRESETS = Collections.unmodifiableList(Arrays.asList(
		new DarkAreaPreset("Asgarnian Ice Dungeon", 11925),
		new DarkAreaPreset("Fremennik Hunter Area", 10810, 10811),
		new DarkAreaPreset("God Wars Dungeon", 11345, 11346, 11347, 11601, 11602, 11603),
		new DarkAreaPreset("Hueycoatl", 5939),
		new DarkAreaPreset("Penguin Agility Course", 10559),
		new DarkAreaPreset("Phantom Muspah", 11330),
		new DarkAreaPreset("Vorkath", 9023),
		new DarkAreaPreset("Weiss", 11325),
		new DarkAreaPreset("Wintertodt", 6461, 6462)
	));

	private final String name;
	private final List<Integer> regionIds;

	private DarkAreaPreset(String name, Integer... regionIds)
	{
		this.name = name;
		this.regionIds = Collections.unmodifiableList(Arrays.asList(regionIds));
	}

	static List<DarkAreaPreset> getPresets()
	{
		return PRESETS;
	}

	String getName()
	{
		return name;
	}

	List<Integer> getRegionIds()
	{
		return regionIds;
	}
}
