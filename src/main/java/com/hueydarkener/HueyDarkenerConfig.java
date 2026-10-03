package com.hueydarkener;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(HueyDarkenerConfig.GROUP)
public interface HueyDarkenerConfig extends Config
{
	String GROUP = "areadarkener";

	@ConfigItem(
		keyName = "globalDarkenEnabled",
		name = "Global Darken",
		description = "Darken every region that is not handled by an enabled Area Darkener entry",
		position = 0
	)
	default boolean globalDarkenEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "globalDarkenStrength",
		name = "Global Darken Strength",
		description = "How strongly to darken regions handled by Global Darken",
		position = 1
	)
	@Range(min = 0, max = 100)
	default int globalDarkenStrength()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "defaultAreaStrength",
		name = "Default Area Strength",
		description = "Starting darkness strength for new Area Darkener entries",
		position = 2
	)
	@Range(min = 0, max = 100)
	default int defaultAreaStrength()
	{
		return 45;
	}
}
