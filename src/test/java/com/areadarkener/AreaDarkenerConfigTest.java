package com.areadarkener;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AreaDarkenerConfigTest
{
	@Test
	public void globalDarkenStrengthDefaultsToFifty()
	{
		AreaDarkenerConfig config = new AreaDarkenerConfig()
		{
		};

		assertEquals(50, config.globalDarkenStrength());
	}
}
