package com.hueydarkener;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.runelite.client.config.ConfigManager;

final class DarkAreaEntryStore
{
	static final String CONFIG_KEY = "areaEntries";
	static final int DEFAULT_AREA_STRENGTH = 50;
	private static final Gson GSON = new Gson();
	private static final Type ENTRY_LIST_TYPE = new TypeToken<List<DarkAreaEntry>>()
	{
	}.getType();

	private final Storage storage;
	private List<DarkAreaEntry> entries;

	DarkAreaEntryStore(ConfigManager configManager)
	{
		this(new ConfigManagerStorage(configManager));
	}

	DarkAreaEntryStore(Storage storage)
	{
		this.storage = storage;
		this.entries = loadEntries();
	}

	List<DarkAreaEntry> getEntries()
	{
		return entries;
	}

	void saveEntries(List<DarkAreaEntry> entries)
	{
		this.entries = sanitize(entries);
		storage.save(GSON.toJson(this.entries, ENTRY_LIST_TYPE));
	}

	Optional<DarkAreaEntry> findEntryContainingRegion(int regionId)
	{
		return entries.stream()
			.filter(entry -> entry.containsRegion(regionId))
			.findFirst();
	}

	OptionalInt findDarknessForRegion(int regionId)
	{
		return entries.stream()
			.filter(DarkAreaEntry::isEnabled)
			.filter(entry -> entry.containsRegion(regionId))
			.mapToInt(DarkAreaEntry::getDarkness)
			.findFirst();
	}

	String nextEntryName()
	{
		int index = 1;
		while (containsEntryName("My Entry " + index))
		{
			index++;
		}
		return "My Entry " + index;
	}

	boolean addCurrentRegionToEntry(DarkAreaEntry entry, int regionId)
	{
		if (regionId <= 0 || findEntryContainingRegion(regionId).isPresent())
		{
			return false;
		}

		List<Integer> regionIds = new ArrayList<>(entry.getRegionIds());
		regionIds.add(regionId);
		entry.setRegionIds(regionIds);
		saveEntries(entries);
		return true;
	}

	DarkAreaEntry createCurrentRegionEntry(int regionId)
	{
		return new DarkAreaEntry(nextEntryName(), List.of(regionId), DEFAULT_AREA_STRENGTH, true);
	}

	private List<DarkAreaEntry> loadEntries()
	{
		String json = storage.load();
		if (json == null || json.trim().isEmpty())
		{
			return new ArrayList<>();
		}

		try
		{
			List<DarkAreaEntry> loaded = GSON.fromJson(json, ENTRY_LIST_TYPE);
			if (loaded == null)
			{
				return new ArrayList<>();
			}

			List<DarkAreaEntry> sanitized = sanitize(loaded);
			return sanitized;
		}
		catch (RuntimeException ex)
		{
			return new ArrayList<>();
		}
	}

	private List<DarkAreaEntry> sanitize(List<DarkAreaEntry> unsanitized)
	{
		List<DarkAreaEntry> sanitized = new ArrayList<>();
		Set<Integer> seenRegionIds = new HashSet<>();
		for (DarkAreaEntry entry : unsanitized)
		{
			if (entry == null)
			{
				continue;
			}

			List<Integer> regionIds = new ArrayList<>();
			if (entry.getRegionIds() == null)
			{
				continue;
			}

			for (Integer regionId : entry.getRegionIds())
			{
				if (regionId != null && regionId > 0 && seenRegionIds.add(regionId))
				{
					regionIds.add(regionId);
				}
			}

			if (regionIds.isEmpty())
			{
				continue;
			}

			String name = entry.getName() == null || entry.getName().trim().isEmpty() ? nextFallbackName(sanitized.size()) : entry.getName().trim();
			sanitized.add(new DarkAreaEntry(name, regionIds, clamp(entry.getDarkness()), entry.isEnabled()));
		}
		return sanitized;
	}

	private boolean containsEntryName(String name)
	{
		return entries.stream().anyMatch(entry -> entry.getName().equals(name));
	}

	private static int clamp(int value)
	{
		return Math.max(0, Math.min(100, value));
	}

	private static String nextFallbackName(int index)
	{
		return "My Entry " + (index + 1);
	}

	interface Storage
	{
		String load();

		void save(String value);
	}

	private static final class ConfigManagerStorage implements Storage
	{
		private final ConfigManager configManager;

		private ConfigManagerStorage(ConfigManager configManager)
		{
			this.configManager = configManager;
		}

		@Override
		public String load()
		{
			return configManager.getConfiguration(HueyDarkenerConfig.GROUP, CONFIG_KEY);
		}

		@Override
		public void save(String value)
		{
			configManager.setConfiguration(HueyDarkenerConfig.GROUP, CONFIG_KEY, value);
		}
	}
}
