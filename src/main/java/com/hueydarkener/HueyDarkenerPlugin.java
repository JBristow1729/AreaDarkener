package com.hueydarkener;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PreMapLoad;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Huey Darkener",
	description = "Darkens Hueycoatl terrain and scenery colors",
	tags = {"huey", "hueycoatl", "visuals", "graphics", "recolor"}
)
public class HueyDarkenerPlugin extends Plugin
{
	private static final int NEXT_REFRESH_UNSET = -1;
	private static final int MAX_HSL = 0xFFFF;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private HueyDarkenerConfig config;

	private final int[] remappedHsl = new int[MAX_HSL + 1];
	private final Set<Renderable> processedRenderables = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Model> processedModels = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<Model, ModelSnapshot> modelSnapshots = new IdentityHashMap<>();
	private int nextReloadTick = NEXT_REFRESH_UNSET;

	@Override
	protected void startUp()
	{
		updateColorMap();
		triggerMapReload(false);
	}

	@Override
	protected void shutDown()
	{
		triggerMapReload(true);
	}

	@Subscribe
	public void onPreMapLoad(PreMapLoad preMapLoad)
	{
		updateColorMap();
		recolorMap(preMapLoad.getScene());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!HueyDarkenerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		updateColorMap();
		nextReloadTick = client.getTickCount() + 1;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (nextReloadTick != NEXT_REFRESH_UNSET && client.getTickCount() >= nextReloadTick)
		{
			triggerMapReload(true);
			nextReloadTick = NEXT_REFRESH_UNSET;
		}
	}

	@Provides
	HueyDarkenerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HueyDarkenerConfig.class);
	}

	private void triggerMapReload(boolean restoreSnapshotsFirst)
	{
		clientThread.invokeLater(() ->
		{
			if (restoreSnapshotsFirst)
			{
				restoreSnapshots();
			}
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	private void recolorMap(Scene scene)
	{
		processedRenderables.clear();
		processedModels.clear();
		Tile[][][] tiles = scene.isInstance() ? scene.getTiles() : scene.getExtendedTiles();
		if (tiles == null)
		{
			return;
		}

		for (Tile[][] zTiles : tiles)
		{
			for (Tile[] xTiles : zTiles)
			{
				for (Tile tile : xTiles)
				{
					if (canRecolorTile(scene, tile))
					{
						recolorTile(tile);
					}
				}
			}
		}
	}

	private boolean canRecolorTile(Scene scene, Tile tile)
	{
		if (tile == null)
		{
			return false;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), tile.getPlane());
		return worldPoint != null && HueyArea.isHueyRegion(worldPoint.getRegionID());
	}

	private void recolorTile(Tile tile)
	{
		Tile current = tile;
		while (current != null)
		{
			recolorTilePaint(current.getSceneTilePaint());
			recolorTileModel(current.getSceneTileModel());
			recolorGroundObject(current.getGroundObject());
			recolorGameObjects(current.getGameObjects());
			recolorRenderable(current.getDecorativeObject() == null ? null : current.getDecorativeObject().getRenderable());
			recolorRenderable(current.getDecorativeObject() == null ? null : current.getDecorativeObject().getRenderable2());
			recolorRenderable(current.getWallObject() == null ? null : current.getWallObject().getRenderable1());
			recolorRenderable(current.getWallObject() == null ? null : current.getWallObject().getRenderable2());
			current = current.getBridge();
		}
	}

	private void recolorTilePaint(SceneTilePaint paint)
	{
		if (paint == null || paint.getTexture() != -1)
		{
			return;
		}

		paint.setNwColor(remappedHsl(paint.getNwColor()));
		paint.setNeColor(remappedHsl(paint.getNeColor()));
		paint.setSwColor(remappedHsl(paint.getSwColor()));
		paint.setSeColor(remappedHsl(paint.getSeColor()));
	}

	private void recolorTileModel(SceneTileModel model)
	{
		if (model == null)
		{
			return;
		}

		adjustColors(model.getTriangleColorA(), model.getTriangleTextureId());
		adjustColors(model.getTriangleColorB(), model.getTriangleTextureId());
		adjustColors(model.getTriangleColorC(), model.getTriangleTextureId());
	}

	private void recolorGroundObject(GroundObject groundObject)
	{
		recolorRenderable(groundObject == null ? null : groundObject.getRenderable());
	}

	private void recolorGameObjects(GameObject[] gameObjects)
	{
		if (gameObjects == null)
		{
			return;
		}

		for (GameObject gameObject : gameObjects)
		{
			recolorRenderable(gameObject == null ? null : gameObject.getRenderable());
		}
	}

	private void recolorRenderable(Renderable renderable)
	{
		if (!(renderable instanceof Model) || !processedRenderables.add(renderable))
		{
			return;
		}

		Model model = (Model) renderable;
		if (!processedModels.add(model))
		{
			return;
		}

		modelSnapshots.computeIfAbsent(model, ModelSnapshot::new);
		adjustColors(model.getFaceColors1(), null);
		adjustColors(model.getFaceColors2(), null);
		adjustColors(model.getFaceColors3(), null);
		adjustShortColors(model.getUnlitFaceColors());
	}

	private void adjustColors(int[] colors, int[] textures)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length; i++)
		{
			if (textures == null || textures.length <= i || textures[i] == -1)
			{
				colors[i] = remappedHsl(colors[i]);
			}
		}
	}

	private void adjustShortColors(short[] colors)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length; i++)
		{
			colors[i] = (short) remappedHsl(colors[i] & MAX_HSL);
		}
	}

	private void updateColorMap()
	{
		if (client.getGameState() != GameState.LOGGED_IN && client.getGameState() != GameState.LOADING)
		{
			return;
		}

		for (int hsl = 0; hsl < remappedHsl.length; hsl++)
		{
			remappedHsl[hsl] = HslDarkener.darkenPackedHsl(hsl, config.darknessStrength());
		}
	}

	private int remappedHsl(int hsl)
	{
		if (hsl < 0 || hsl >= remappedHsl.length)
		{
			return hsl;
		}

		return remappedHsl[hsl];
	}

	private void restoreSnapshots()
	{
		for (Map.Entry<Model, ModelSnapshot> entry : modelSnapshots.entrySet())
		{
			entry.getValue().restore(entry.getKey());
		}
		modelSnapshots.clear();
	}

	private static void restoreArray(int[] target, int[] source)
	{
		if (target != null && source != null && target.length == source.length)
		{
			System.arraycopy(source, 0, target, 0, source.length);
		}
	}

	private static void restoreArray(short[] target, short[] source)
	{
		if (target != null && source != null && target.length == source.length)
		{
			System.arraycopy(source, 0, target, 0, source.length);
		}
	}

	private static final class ModelSnapshot
	{
		private final int[] faceColors1;
		private final int[] faceColors2;
		private final int[] faceColors3;
		private final short[] unlitFaceColors;

		private ModelSnapshot(Model model)
		{
			faceColors1 = model.getFaceColors1() == null ? null : model.getFaceColors1().clone();
			faceColors2 = model.getFaceColors2() == null ? null : model.getFaceColors2().clone();
			faceColors3 = model.getFaceColors3() == null ? null : model.getFaceColors3().clone();
			unlitFaceColors = model.getUnlitFaceColors() == null ? null : model.getUnlitFaceColors().clone();
		}

		private void restore(Model model)
		{
			restoreArray(model.getFaceColors1(), faceColors1);
			restoreArray(model.getFaceColors2(), faceColors2);
			restoreArray(model.getFaceColors3(), faceColors3);
			restoreArray(model.getUnlitFaceColors(), unlitFaceColors);
		}
	}
}
