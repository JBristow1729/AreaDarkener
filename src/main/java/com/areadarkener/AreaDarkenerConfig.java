package com.areadarkener;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AreaDarkenerConfig.GROUP)
public interface AreaDarkenerConfig extends Config
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
		return 50;
	}
}
