package com.hueydarkener;

import java.util.Arrays;
import java.util.Optional;

enum DarkArea
{
	HUEYCOATL(5939),
	WINTERTODT(6461, 6462),
	GOD_WARS_DUNGEON(11345, 11346, 11347, 11601, 11602, 11603),
	VORKATH(9023),
	FREMENNIK_HUNTER_AREA(10810, 10811),
	PENGUIN_AGILITY_COURSE(10559),
	WEISS(11325),
	ASGARNIAN_ICE_DUNGEON(11925);

	private final int[] regionIds;

	DarkArea(int... regionIds)
	{
		this.regionIds = regionIds;
	}

	static Optional<DarkArea> findByRegionId(int regionId)
	{
		return Arrays.stream(values())
			.filter(area -> area.containsRegion(regionId))
			.findFirst();
	}

	private boolean containsRegion(int regionId)
	{
		return Arrays.stream(regionIds).anyMatch(id -> id == regionId);
	}
}
