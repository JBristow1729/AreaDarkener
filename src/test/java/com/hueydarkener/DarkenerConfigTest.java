package com.hueydarkener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;

public class DarkenerConfigTest
{
	@Test
	public void disabledAreaDoesNotReturnSettings() throws Exception
	{
		HueyDarkenerPlugin plugin = pluginWithConfig(new HueyDarkenerConfig()
		{
			@Override
			public boolean wintertodtEnabled()
			{
				return false;
			}
		});

		assertNull(settingsForRegion(plugin, 6461));
	}

	@Test
	public void customRegionIdsReturnSettings() throws Exception
	{
		HueyDarkenerPlugin plugin = pluginWithConfig(new HueyDarkenerConfig()
		{
			@Override
			public String customRegionIds()
			{
				return "12345, 23456 invalid";
			}
		});

		assertNotNull(settingsForRegion(plugin, 23456));
		assertNull(settingsForRegion(plugin, 34567));
	}

	private static HueyDarkenerPlugin pluginWithConfig(HueyDarkenerConfig config) throws Exception
	{
		HueyDarkenerPlugin plugin = new HueyDarkenerPlugin();
		Field configField = HueyDarkenerPlugin.class.getDeclaredField("config");
		configField.setAccessible(true);
		configField.set(plugin, config);
		return plugin;
	}

	private static Object settingsForRegion(HueyDarkenerPlugin plugin, int regionId) throws Exception
	{
		Method method = HueyDarkenerPlugin.class.getDeclaredMethod("settingsForRegion", int.class);
		method.setAccessible(true);
		return method.invoke(plugin, regionId);
	}
}
