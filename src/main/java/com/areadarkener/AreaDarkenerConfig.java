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
		description = "This will darken everything everywhere in RuneScape. It is better to take the time to set up the regions you want to be darker via the side panel.",
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

	@ConfigItem(
		keyName = "boundaryLines",
		name = "Boundary Lines",
		description = "Draw region boundaries on the ground to help you add neighbouring regions via the side panel",
		position = 2
	)
	default boolean boundaryLines()
	{
		return false;
	}
}
