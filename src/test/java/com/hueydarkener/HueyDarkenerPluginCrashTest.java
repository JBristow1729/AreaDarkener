package com.hueydarkener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.runelite.api.Renderable;
import org.junit.Test;

public class HueyDarkenerPluginCrashTest
{
	@Test
	public void doesNotRequestModelsFromNonModelRenderablesDuringMapLoad() throws Exception
	{
		HueyDarkenerPlugin plugin = new HueyDarkenerPlugin();
		Renderable renderable = (Renderable) Proxy.newProxyInstance(
			Renderable.class.getClassLoader(),
			new Class<?>[]{Renderable.class},
			(proxy, method, args) ->
			{
				if ("getModel".equals(method.getName()))
				{
					throw new AssertionError("must be called on client thread");
				}
				if ("toString".equals(method.getName()))
				{
					return "non-model renderable";
				}
				if (method.getReturnType() == int.class)
				{
					return 0;
				}
				return null;
			}
		);

		Method recolorRenderable = HueyDarkenerPlugin.class.getDeclaredMethod("recolorRenderable", Renderable.class);
		recolorRenderable.setAccessible(true);

		try
		{
			recolorRenderable.invoke(plugin, renderable);
		}
		catch (InvocationTargetException ex)
		{
			Throwable cause = ex.getCause();
			if (cause instanceof Error)
			{
				throw (Error) cause;
			}
			throw (Exception) cause;
		}
	}
}
