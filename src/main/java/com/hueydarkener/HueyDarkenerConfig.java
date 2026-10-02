package com.hueydarkener;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(HueyDarkenerConfig.GROUP)
public interface HueyDarkenerConfig extends Config
{
	String GROUP = "darkener";

	@ConfigSection(
		name = "Hueycoatl",
		description = "The Hueycoatl arena",
		position = 0
	)
	String hueycoatlSection = "hueycoatl";

	@ConfigSection(
		name = "Wintertodt",
		description = "The Wintertodt arena",
		position = 1
	)
	String wintertodtSection = "wintertodt";

	@ConfigSection(
		name = "God Wars Dungeon",
		description = "The snowy God Wars Dungeon areas",
		position = 2
	)
	String godWarsDungeonSection = "godWarsDungeon";

	@ConfigSection(
		name = "Vorkath",
		description = "Vorkath's island arena",
		position = 3
	)
	String vorkathSection = "vorkath";

	@ConfigSection(
		name = "Fremennik Hunter Area",
		description = "The snowy Rellekka and Trollweiss hunter area near DKS",
		position = 4
	)
	String fremennikHunterAreaSection = "fremennikHunterArea";

	@ConfigSection(
		name = "Penguin Agility Course",
		description = "The Penguin Agility Course on the Iceberg",
		position = 5
	)
	String penguinAgilityCourseSection = "penguinAgilityCourse";

	@ConfigSection(
		name = "Weiss",
		description = "The snowy Weiss area",
		position = 6
	)
	String weissSection = "weiss";

	@ConfigSection(
		name = "Phantom Muspah",
		description = "The Phantom Muspah lair",
		position = 7
	)
	String phantomMuspahSection = "phantomMuspah";

	@ConfigSection(
		name = "Asgarnian Ice Dungeon",
		description = "The Asgarnian Ice Dungeon",
		position = 8
	)
	String asgarnianIceDungeonSection = "asgarnianIceDungeon";

	@ConfigSection(
		name = "Custom regions",
		description = "Optional extra region IDs to darken",
		position = 9
	)
	String customRegionsSection = "customRegions";

	@ConfigItem(
		keyName = "hueycoatlEnabled",
		name = "Hueycoatl",
		description = "Darken the Hueycoatl arena",
		section = hueycoatlSection,
		position = 0
	)
	default boolean hueycoatlEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hueycoatlDarkness",
		name = "Strength",
		description = "How strongly to darken Hueycoatl",
		section = hueycoatlSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int hueycoatlDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "wintertodtEnabled",
		name = "Wintertodt",
		description = "Darken Wintertodt",
		section = wintertodtSection,
		position = 0
	)
	default boolean wintertodtEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wintertodtDarkness",
		name = "Strength",
		description = "How strongly to darken Wintertodt",
		section = wintertodtSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int wintertodtDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "godWarsDungeonEnabled",
		name = "GWD",
		description = "Darken God Wars Dungeon",
		section = godWarsDungeonSection,
		position = 0
	)
	default boolean godWarsDungeonEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "godWarsDungeonDarkness",
		name = "Strength",
		description = "How strongly to darken God Wars Dungeon",
		section = godWarsDungeonSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int godWarsDungeonDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "vorkathEnabled",
		name = "Vorkath",
		description = "Darken Vorkath",
		section = vorkathSection,
		position = 0
	)
	default boolean vorkathEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "vorkathDarkness",
		name = "Strength",
		description = "How strongly to darken Vorkath",
		section = vorkathSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int vorkathDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "fremennikHunterAreaEnabled",
		name = "Fremennik Hunter Area",
		description = "Darken the snowy Rellekka and Trollweiss hunter area near DKS",
		section = fremennikHunterAreaSection,
		position = 0
	)
	default boolean fremennikHunterAreaEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fremennikHunterAreaDarkness",
		name = "Strength",
		description = "How strongly to darken the Fremennik Hunter Area",
		section = fremennikHunterAreaSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int fremennikHunterAreaDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "penguinAgilityCourseEnabled",
		name = "Penguin Agility Course",
		description = "Darken the Penguin Agility Course",
		section = penguinAgilityCourseSection,
		position = 0
	)
	default boolean penguinAgilityCourseEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "penguinAgilityCourseDarkness",
		name = "Strength",
		description = "How strongly to darken the Penguin Agility Course",
		section = penguinAgilityCourseSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int penguinAgilityCourseDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "weissEnabled",
		name = "Weiss",
		description = "Darken Weiss",
		section = weissSection,
		position = 0
	)
	default boolean weissEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weissDarkness",
		name = "Strength",
		description = "How strongly to darken Weiss",
		section = weissSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int weissDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "phantomMuspahEnabled",
		name = "Phantom Muspah",
		description = "Darken the Phantom Muspah lair",
		section = phantomMuspahSection,
		position = 0
	)
	default boolean phantomMuspahEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "phantomMuspahDarkness",
		name = "Strength",
		description = "How strongly to darken the Phantom Muspah lair",
		section = phantomMuspahSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int phantomMuspahDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "asgarnianIceDungeonEnabled",
		name = "Asgarnian Ice Dungeon",
		description = "Darken the Asgarnian Ice Dungeon",
		section = asgarnianIceDungeonSection,
		position = 0
	)
	default boolean asgarnianIceDungeonEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "asgarnianIceDungeonDarkness",
		name = "Strength",
		description = "How strongly to darken the Asgarnian Ice Dungeon",
		section = asgarnianIceDungeonSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int asgarnianIceDungeonDarkness()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "customRegionIds",
		name = "Region IDs",
		description = "Comma-separated region IDs for any other bright areas. Use RuneLite's Region ID plugin to find IDs.",
		section = customRegionsSection,
		position = 0
	)
	default String customRegionIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "customRegionsDarkness",
		name = "Strength",
		description = "How strongly to darken custom regions",
		section = customRegionsSection,
		position = 1
	)
	@Range(min = 0, max = 100)
	default int customRegionsDarkness()
	{
		return 45;
	}
}
