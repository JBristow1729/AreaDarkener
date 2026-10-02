package com.hueydarkener;

import java.util.Set;

final class HueyArea
{
	private static final Set<Integer> HUEY_REGION_IDS = Set.of(5939);

	private HueyArea()
	{
	}

	static boolean isHueyRegion(int regionId)
	{
		return HUEY_REGION_IDS.contains(regionId);
	}
}
