package com.areadarkener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Displays boundaries from template region IDs, including rotated instance chunks. */
final class RegionBoundaryOverlay extends Overlay
{
	private static final Color LINE_COLOR = new Color(255, 160, 40, 230);
	private static final BasicStroke LINE_STROKE = new BasicStroke(2);
	private final Client client;
	private final AreaDarkenerConfig config;
	private List<RegionBoundaries.Edge> edges = Collections.emptyList();
	private WorldView worldView;
	private int plane;

	RegionBoundaryOverlay(Client client, AreaDarkenerConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		// This layer follows the scene tint, but is still beneath interface widgets.
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	void clear()
	{
		edges = Collections.emptyList();
		worldView = null;
	}

	void refresh()
	{
		clear();
		if (!config.boundaryLines() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		WorldView view = client.getTopLevelWorldView();
		if (view == null)
		{
			return;
		}
		Scene scene = view.getScene();
		int currentPlane = view.getPlane();
		Tile[][][] tiles = scene.getTiles();
		if (tiles == null || currentPlane < 0 || currentPlane >= tiles.length || tiles[currentPlane] == null)
		{
			return;
		}
		Tile[][] floor = tiles[currentPlane];
		int[][] regions = new int[floor.length][];
		for (int x = 0; x < floor.length; x++)
		{
			regions[x] = new int[floor[x] == null ? 0 : floor[x].length];
			Arrays.fill(regions[x], -1);
			for (int y = 0; y < regions[x].length; y++)
			{
				Tile tile = floor[x][y];
				if (tile != null)
				{
					WorldPoint point = WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), currentPlane);
					if (point != null)
					{
						regions[x][y] = point.getRegionID();
					}
				}
			}
		}
		edges = RegionBoundaries.find(regions);
		worldView = view;
		plane = currentPlane;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.boundaryLines() || client.getGameState() != GameState.LOGGED_IN
			|| worldView == null || client.getTopLevelWorldView() != worldView || worldView.getPlane() != plane)
		{
			return null;
		}
		graphics.setColor(LINE_COLOR);
		graphics.setStroke(LINE_STROKE);
		for (RegionBoundaries.Edge edge : edges)
		{
			Point start = project(edge.x1, edge.y1);
			Point end = project(edge.x2, edge.y2);
			if (start != null && end != null)
			{
				graphics.drawLine(start.getX(), start.getY(), end.getX(), end.getY());
			}
		}
		return null;
	}

	private Point project(int x, int y)
	{
		// Tile corners lie at multiples of 128; fromScene() instead returns tile centres.
		LocalPoint corner = new LocalPoint(x * Perspective.LOCAL_TILE_SIZE, y * Perspective.LOCAL_TILE_SIZE, worldView);
		return Perspective.localToCanvas(client, corner, plane);
	}
}
