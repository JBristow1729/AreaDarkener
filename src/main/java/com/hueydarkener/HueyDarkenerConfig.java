package com.hueydarkener;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(HueyDarkenerConfig.GROUP)
public interface HueyDarkenerConfig extends Config
{
	String GROUP = "hueydarkener";

	@ConfigItem(
		keyName = "darknessStrength",
		name = "Darkness Strength",
		description = "How strongly to darken Hueycoatl terrain and scenery colors"
	)
	@Range(
		min = 0,
		max = 100
	)
	default int darknessStrength()
	{
		return 45;
	}
}
