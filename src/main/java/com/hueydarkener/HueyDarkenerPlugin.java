package com.hueydarkener;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.Player;
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
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Area Darkener",
	description = "Darkens bright terrain and scenery colors",
	tags = {"dark", "terrain", "wintertodt", "gwd", "huey", "vorkath", "visuals", "graphics", "recolor"}
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

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private final Map<Integer, int[]> remappedHslByStrength = new HashMap<>();
	private final Set<Renderable> processedRenderables = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Model> processedModels = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<Model, ModelSnapshot> modelSnapshots = new IdentityHashMap<>();
	private DarkAreaEntryStore entryStore;
	private AreaDarkenerPanel panel;
	private NavigationButton navigationButton;
	private int nextReloadTick = NEXT_REFRESH_UNSET;
	private OptionalInt latestRegionId = OptionalInt.empty();

	@Override
	protected void startUp()
	{
		entryStore = new DarkAreaEntryStore(configManager);
		panel = new AreaDarkenerPanel(entryStore, this::currentRegionId, this::requestReload);
		navigationButton = NavigationButton.builder()
			.tooltip("Area Darkener")
			.icon(createIcon())
			.panel(panel)
			.priority(6)
			.build();
		clientToolbar.addNavigation(navigationButton);
		triggerMapReload(false);
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
			panel = null;
		}
		triggerMapReload(true);
	}

	@Subscribe
	public void onPreMapLoad(PreMapLoad preMapLoad)
	{
		recolorMap(preMapLoad.getScene());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!HueyDarkenerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		requestReload();
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		updateLatestRegionId();
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
					AreaSettings settings = findAreaSettings(scene, tile);
					if (settings != null)
					{
						recolorTile(tile, settings.darknessStrength);
					}
				}
			}
		}
	}

	private AreaSettings findAreaSettings(Scene scene, Tile tile)
	{
		if (tile == null)
		{
			return null;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), tile.getPlane());
		if (worldPoint == null)
		{
			return null;
		}

		OptionalInt darkness = darknessForRegion(worldPoint.getRegionID());
		return darkness.isPresent() ? new AreaSettings(darkness.getAsInt()) : null;
	}

	OptionalInt darknessForRegion(int regionId)
	{
		if (entryStore != null)
		{
			OptionalInt entryDarkness = entryStore.findDarknessForRegion(regionId);
			if (entryDarkness.isPresent())
			{
				return entryDarkness;
			}
		}

		return config.globalDarkenEnabled() ? OptionalInt.of(config.globalDarkenStrength()) : OptionalInt.empty();
	}

	void requestReload()
	{
		clientThread.invokeLater(() ->
		{
			remappedHslByStrength.clear();
			nextReloadTick = client.getTickCount() + 1;
		});
	}

	OptionalInt currentRegionId()
	{
		return latestRegionId;
	}

	private void updateLatestRegionId()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			latestRegionId = OptionalInt.empty();
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			latestRegionId = OptionalInt.empty();
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation());
		latestRegionId = worldPoint == null ? OptionalInt.empty() : OptionalInt.of(worldPoint.getRegionID());
	}

	private void recolorTile(Tile tile, int darknessStrength)
	{
		Tile current = tile;
		while (current != null)
		{
			recolorTilePaint(current.getSceneTilePaint(), darknessStrength);
			recolorTileModel(current.getSceneTileModel(), darknessStrength);
			recolorGroundObject(current.getGroundObject(), darknessStrength);
			recolorGameObjects(current.getGameObjects(), darknessStrength);
			recolorRenderable(current.getDecorativeObject() == null ? null : current.getDecorativeObject().getRenderable(), darknessStrength);
			recolorRenderable(current.getDecorativeObject() == null ? null : current.getDecorativeObject().getRenderable2(), darknessStrength);
			recolorRenderable(current.getWallObject() == null ? null : current.getWallObject().getRenderable1(), darknessStrength);
			recolorRenderable(current.getWallObject() == null ? null : current.getWallObject().getRenderable2(), darknessStrength);
			current = current.getBridge();
		}
	}

	private void recolorTilePaint(SceneTilePaint paint, int darknessStrength)
	{
		if (paint == null || paint.getTexture() != -1)
		{
			return;
		}

		paint.setNwColor(remappedHsl(paint.getNwColor(), darknessStrength));
		paint.setNeColor(remappedHsl(paint.getNeColor(), darknessStrength));
		paint.setSwColor(remappedHsl(paint.getSwColor(), darknessStrength));
		paint.setSeColor(remappedHsl(paint.getSeColor(), darknessStrength));
	}

	private void recolorTileModel(SceneTileModel model, int darknessStrength)
	{
		if (model == null)
		{
			return;
		}

		adjustColors(model.getTriangleColorA(), model.getTriangleTextureId(), darknessStrength);
		adjustColors(model.getTriangleColorB(), model.getTriangleTextureId(), darknessStrength);
		adjustColors(model.getTriangleColorC(), model.getTriangleTextureId(), darknessStrength);
	}

	private void recolorGroundObject(GroundObject groundObject, int darknessStrength)
	{
		recolorRenderable(groundObject == null ? null : groundObject.getRenderable(), darknessStrength);
	}

	private void recolorGameObjects(GameObject[] gameObjects, int darknessStrength)
	{
		if (gameObjects == null)
		{
			return;
		}

		for (GameObject gameObject : gameObjects)
		{
			recolorRenderable(gameObject == null ? null : gameObject.getRenderable(), darknessStrength);
		}
	}

	private void recolorRenderable(Renderable renderable, int darknessStrength)
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
		adjustColors(model.getFaceColors1(), null, darknessStrength);
		adjustColors(model.getFaceColors2(), null, darknessStrength);
		adjustColors(model.getFaceColors3(), null, darknessStrength);
		adjustShortColors(model.getUnlitFaceColors(), darknessStrength);
	}

	private void adjustColors(int[] colors, int[] textures, int darknessStrength)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length; i++)
		{
			if (textures == null || textures.length <= i || textures[i] == -1)
			{
				colors[i] = remappedHsl(colors[i], darknessStrength);
			}
		}
	}

	private void adjustShortColors(short[] colors, int darknessStrength)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length; i++)
		{
			colors[i] = (short) remappedHsl(colors[i] & MAX_HSL, darknessStrength);
		}
	}

	private int remappedHsl(int hsl, int darknessStrength)
	{
		if (hsl < 0 || hsl > MAX_HSL)
		{
			return hsl;
		}

		int[] remappedHsl = remappedHslByStrength.computeIfAbsent(darknessStrength, this::buildColorMap);
		return remappedHsl[hsl];
	}

	private int[] buildColorMap(int darknessStrength)
	{
		int[] remappedHsl = new int[MAX_HSL + 1];
		for (int hsl = 0; hsl < remappedHsl.length; hsl++)
		{
			remappedHsl[hsl] = HslDarkener.darkenPackedHsl(hsl, darknessStrength);
		}
		return remappedHsl;
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

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(220, 220, 220));
		graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 14f));
		graphics.drawString("D", 3, 13);
		graphics.dispose();
		return image;
	}

	private static final class AreaSettings
	{
		private final int darknessStrength;

		private AreaSettings(int darknessStrength)
		{
			this.darknessStrength = darknessStrength;
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
