package com.areadarkener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

public class AreaDarkenerThreadingTest
{
	@Test
	public void requestReloadSchedulesClientStateChangesOnClientThread() throws Exception
	{
		AreaDarkenerPlugin plugin = new AreaDarkenerPlugin();
		RecordingClientThread clientThread = new RecordingClientThread();
		setField(plugin, "clientThread", clientThread);
		setField(plugin, "client", throwingClient());

		plugin.requestReload();

		assertEquals(1, clientThread.invokeLaterCalls);
	}

	@Test
	public void currentRegionIdDoesNotReadClientFromCallerThread() throws Exception
	{
		AreaDarkenerPlugin plugin = new AreaDarkenerPlugin();
		setField(plugin, "client", throwingClient());

		assertFalse(plugin.currentRegionId().isPresent());
	}

	@Test
	public void gameTickDoesNotRefreshCurrentRegionWhenPanelIsHidden() throws Exception
	{
		AreaDarkenerPlugin plugin = new AreaDarkenerPlugin();
		setField(plugin, "client", throwingClient());

		plugin.onGameTick(new GameTick());
	}

	private static Client throwingClient()
	{
		return (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				throw new AssertionError("client should only be read on the client thread: " + method.getName());
			}
		);
	}

	private static void setField(Object target, String name, Object value) throws Exception
	{
		Field field = AreaDarkenerPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class RecordingClientThread extends ClientThread
	{
		private int invokeLaterCalls;

		@Override
		public void invokeLater(Runnable runnable)
		{
			invokeLaterCalls++;
		}
	}
}
