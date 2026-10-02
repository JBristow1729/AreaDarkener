package com.hueydarkener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HueyAreaTest
{
	@Test
	public void includesHueycoatlArenaRegion()
	{
		assertTrue(HueyArea.isHueyRegion(5939));
	}

	@Test
	public void excludesOtherBrightBossRegions()
	{
		assertFalse(HueyArea.isHueyRegion(6461));
		assertFalse(HueyArea.isHueyRegion(6462));
	}
}
