package com.areadarkener;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntFunction;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

/** Original region-based HSL recolouring, applied before scene upload. */
final class TileRecolourer
{
	private static final int MAX_HSL = 0xFFFF;
	private final IntFunction<OptionalInt> darknessLookup;
	private final Map<Integer, int[]> remappedHslByStrength = new HashMap<>();
	private final Set<Renderable> processedRenderables = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Model> processedModels = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<Model, ModelSnapshot> modelSnapshots = new IdentityHashMap<>();

	TileRecolourer(IntFunction<OptionalInt> darknessLookup)
	{
		this.darknessLookup = darknessLookup;
	}

	void restore()
	{
		clearRecolorState();
	}

	void recolor(Scene scene)
	{
		clearRecolorState();
		Tile[][][] tiles = scene.isInstance() ? scene.getTiles() : scene.getExtendedTiles();
		if (tiles == null)
		{
			return;
		}

		for (Tile[][] zTiles : tiles)
		{
			if (zTiles == null)
			{
				continue;
			}
			for (Tile[] xTiles : zTiles)
			{
				if (xTiles == null)
				{
					continue;
				}
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

		OptionalInt darkness = darknessLookup.apply(worldPoint.getRegionID());
		return darkness.isPresent() ? new AreaSettings(darkness.getAsInt()) : null;
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

	private void clearRecolorState()
	{
		processedRenderables.clear();
		processedModels.clear();
		remappedHslByStrength.clear();
		restoreSnapshots();
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
