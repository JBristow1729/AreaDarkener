package com.hueydarkener;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HueyDarkenerConfigTest
{
	@Test
	public void globalDarkenStrengthDefaultsToFifty()
	{
		HueyDarkenerConfig config = new HueyDarkenerConfig()
		{
		};

		assertEquals(50, config.globalDarkenStrength());
	}
}
