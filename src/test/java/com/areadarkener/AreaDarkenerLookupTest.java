package com.areadarkener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalInt;
import org.junit.Test;

public class AreaDarkenerLookupTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void entryDarknessOverridesGlobalDarkness() throws Exception
	{
		AreaDarkenerPlugin plugin = pluginWith(
			config(true, 80),
			"[{\"name\":\"Vorkath\",\"regionIds\":[9023],\"darkness\":60,\"enabled\":true}]"
		);

		assertEquals(60, darknessForRegion(plugin, 9023).orElseThrow(AssertionError::new));
	}

	@Test
	public void globalDarknessAppliesWhenNoEntryMatches() throws Exception
	{
		AreaDarkenerPlugin plugin = pluginWith(config(true, 80), "[]");

		assertEquals(80, darknessForRegion(plugin, 12345).orElseThrow(AssertionError::new));
	}

	@Test
	public void disabledGlobalDarknessDoesNotApplyWithoutEntry() throws Exception
	{
		AreaDarkenerPlugin plugin = pluginWith(config(false, 80), "[]");

		assertFalse(darknessForRegion(plugin, 12345).isPresent());
	}

	private static AreaDarkenerPlugin pluginWith(AreaDarkenerConfig config, String entriesJson) throws Exception
	{
		AreaDarkenerPlugin plugin = new AreaDarkenerPlugin();
		setField(plugin, "config", config);
		setField(plugin, "entryStore", new DarkAreaEntryStore(new TestStorage(entriesJson), GSON));
		return plugin;
	}

	private static OptionalInt darknessForRegion(AreaDarkenerPlugin plugin, int regionId) throws Exception
	{
		Method method = AreaDarkenerPlugin.class.getDeclaredMethod("darknessForRegion", int.class);
		method.setAccessible(true);
		return (OptionalInt) method.invoke(plugin, regionId);
	}

	private static AreaDarkenerConfig config(boolean globalEnabled, int globalStrength)
	{
		return new AreaDarkenerConfig()
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
		Field field = AreaDarkenerPlugin.class.getDeclaredField(name);
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
