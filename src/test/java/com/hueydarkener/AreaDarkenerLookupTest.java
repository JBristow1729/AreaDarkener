package com.hueydarkener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalInt;
import org.junit.Test;

public class AreaDarkenerLookupTest
{
	@Test
	public void entryDarknessOverridesGlobalDarkness() throws Exception
	{
		HueyDarkenerPlugin plugin = pluginWith(
			config(true, 80),
			"[{\"name\":\"Vorkath\",\"regionIds\":[9023],\"darkness\":60,\"enabled\":true}]"
		);

		assertEquals(60, darknessForRegion(plugin, 9023).orElseThrow(AssertionError::new));
	}

	@Test
	public void globalDarknessAppliesWhenNoEntryMatches() throws Exception
	{
		HueyDarkenerPlugin plugin = pluginWith(config(true, 80), "[]");

		assertEquals(80, darknessForRegion(plugin, 12345).orElseThrow(AssertionError::new));
	}

	@Test
	public void disabledGlobalDarknessDoesNotApplyWithoutEntry() throws Exception
	{
		HueyDarkenerPlugin plugin = pluginWith(config(false, 80), "[]");

		assertFalse(darknessForRegion(plugin, 12345).isPresent());
	}

	private static HueyDarkenerPlugin pluginWith(HueyDarkenerConfig config, String entriesJson) throws Exception
	{
		HueyDarkenerPlugin plugin = new HueyDarkenerPlugin();
		setField(plugin, "config", config);
		setField(plugin, "entryStore", new DarkAreaEntryStore(new TestStorage(entriesJson)));
		return plugin;
	}

	private static OptionalInt darknessForRegion(HueyDarkenerPlugin plugin, int regionId) throws Exception
	{
		Method method = HueyDarkenerPlugin.class.getDeclaredMethod("darknessForRegion", int.class);
		method.setAccessible(true);
		return (OptionalInt) method.invoke(plugin, regionId);
	}

	private static HueyDarkenerConfig config(boolean globalEnabled, int globalStrength)
	{
		return new HueyDarkenerConfig()
		{
			@Override
			public boolean globalDarkenEnabled()
			{
				return globalEnabled;
			}

			@Override
			public int globalDarkenStrength()
			{
				return globalStrength;
			}
		};
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field field = HueyDarkenerPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestStorage implements DarkAreaEntryStore.Storage
	{
		private final String value;

		private TestStorage(String value)
		{
			this.value = value;
		}

		@Override
		public String load()
		{
			return value;
		}

		@Override
		public void save(String value)
		{
		}
	}
}
