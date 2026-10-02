package com.hueydarkener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DarkAreaTest
{
	@Test
	public void mapsSupportedBrightAreasToConfigs()
	{
		assertEquals(DarkArea.HUEYCOATL, DarkArea.findByRegionId(5939).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.WINTERTODT, DarkArea.findByRegionId(6461).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.WINTERTODT, DarkArea.findByRegionId(6462).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.GOD_WARS_DUNGEON, DarkArea.findByRegionId(11345).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.GOD_WARS_DUNGEON, DarkArea.findByRegionId(11603).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.VORKATH, DarkArea.findByRegionId(9023).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.FREMENNIK_HUNTER_AREA, DarkArea.findByRegionId(10810).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.FREMENNIK_HUNTER_AREA, DarkArea.findByRegionId(10811).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.PENGUIN_AGILITY_COURSE, DarkArea.findByRegionId(10559).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.WEISS, DarkArea.findByRegionId(11325).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.PHANTOM_MUSPAH, DarkArea.findByRegionId(11330).orElseThrow(AssertionError::new));
		assertEquals(DarkArea.ASGARNIAN_ICE_DUNGEON, DarkArea.findByRegionId(11925).orElseThrow(AssertionError::new));
	}

	@Test
	public void ignoresUnsupportedRegions()
	{
		assertFalse(DarkArea.findByRegionId(12850).isPresent());
	}
}
