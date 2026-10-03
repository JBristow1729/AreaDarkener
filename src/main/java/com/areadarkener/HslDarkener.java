package com.areadarkener;

final class HslDarkener
{
	private static final int MAX_HSL = 0xFFFF;
	private static final int HSL_HUE_SHIFT = 10;
	private static final int HSL_SATURATION_SHIFT = 7;
	private static final int HSL_HUE_MASK = 0x3F;
	private static final int HSL_SATURATION_MASK = 0x7;
	private static final int HSL_LIGHTNESS_MASK = 0x7F;
	private static final int HSL_HIDDEN_COLOR = 12345678;

	private HslDarkener()
	{
	}

	static int darkenPackedHsl(int packedHsl, int darknessStrength)
	{
		if (packedHsl == HSL_HIDDEN_COLOR || packedHsl < 0 || packedHsl > MAX_HSL)
		{
			return packedHsl;
		}

		int clampedStrength = Math.max(0, Math.min(100, darknessStrength));
		double scale = (100.0 - clampedStrength) / 100.0;
		int hue = (packedHsl >> HSL_HUE_SHIFT) & HSL_HUE_MASK;
		int saturation = (packedHsl >> HSL_SATURATION_SHIFT) & HSL_SATURATION_MASK;
		int lightness = packedHsl & HSL_LIGHTNESS_MASK;
		int newLightness = Math.max(1, Math.min(HSL_LIGHTNESS_MASK, (int) Math.ceil(scale * lightness)));

		return (hue << HSL_HUE_SHIFT) | (saturation << HSL_SATURATION_SHIFT) | newLightness;
	}
}
