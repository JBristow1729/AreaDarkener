package com.areadarkener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Tints the scene before widgets are drawn, including textures and moving actors. */
final class AreaDarkenerOverlay extends Overlay
{
	private final Client client;
	private final AreaDarkenerPlugin plugin;
	private final DarknessTransition transition = new DarknessTransition();

	@Inject
	AreaDarkenerOverlay(Client client, AreaDarkenerPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		long now = System.nanoTime();
		transition.setTarget(plugin.currentDarkness(), now);
		int alpha = (int) Math.round(255 * transition.valueAt(now) / 100);
		if (alpha > 0)
		{
			graphics.setColor(new Color(0, 0, 0, alpha));
			graphics.fillRect(client.getViewportXOffset(), client.getViewportYOffset(),
				client.getViewportWidth(), client.getViewportHeight());
		}
		return null;
	}
}
