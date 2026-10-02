package com.hueydarkener;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HslDarkenerTest
{
	@Test
	public void darkensPackedHslByScalingLightnessOnly()
	{
		int hue = 12;
		int saturation = 5;
		int lightness = 80;
		int packedHsl = (hue << 10) | (saturation << 7) | lightness;

		int darkened = HslDarkener.darkenPackedHsl(packedHsl, 45);

		assertEquals((hue << 10) | (saturation << 7) | 44, darkened);
	}

	@Test
	public void keepsVisibleColorsAtLeastOneLightness()
	{
		int packedHsl = (3 << 10) | (2 << 7) | 1;

		assertEquals(packedHsl, HslDarkener.darkenPackedHsl(packedHsl, 100));
	}

	@Test
	public void doesNotChangeHiddenOrInvalidColors()
	{
		assertEquals(12345678, HslDarkener.darkenPackedHsl(12345678, 45));
		assertEquals(-1, HslDarkener.darkenPackedHsl(-1, 45));
		assertEquals(0x1FFFF, HslDarkener.darkenPackedHsl(0x1FFFF, 45));
	}
}
