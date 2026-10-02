package com.hueydarkener;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HueyDarkenerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HueyDarkenerPlugin.class);
		RuneLite.main(args);
	}
}
