package com.areadarkener;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalInt;
import javax.inject.Inject;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
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
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Area Darkener",
	description = "A plugin for darkening any region in RuneScape. Created as an accessibility plugin but useful for all.",
	tags = {"dark", "area", "terrain", "accessibility", "wintertodt", "gwd", "huey", "vorkath", "visuals", "graphics", "recolor"}
)
public class AreaDarkenerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private AreaDarkenerConfig config;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OverlayManager overlayManager;

	private DarkAreaEntryStore entryStore;
	private AreaDarkenerPanel panel;
	private NavigationButton navigationButton;
	private AreaDarkenerOverlay overlay;
	private RegionBoundaryOverlay boundaryOverlay;
	private final TileRecolourer tileRecolourer = new TileRecolourer(this::darknessForRegion);
	private boolean tileRecolourMode;
	private boolean started;
	private boolean rebuildAfterLogin;
	private int nextReloadTick = -1;
	private volatile OptionalInt latestRegionId = OptionalInt.empty();

	@Override
	protected void startUp()
	{
		started = true;
		entryStore = new DarkAreaEntryStore(configManager, gson);
		panel = new AreaDarkenerPanel(entryStore, this::currentRegionId, this::requestReload);
		navigationButton = NavigationButton.builder()
			.tooltip("Area Darkener").icon(createIcon()).panel(panel).priority(6).build();
		clientToolbar.addNavigation(navigationButton);
		overlay = new AreaDarkenerOverlay(client, this);
		overlayManager.add(overlay);
		boundaryOverlay = new RegionBoundaryOverlay(client, config);
		overlayManager.add(boundaryOverlay);
		requestReload();
	}

	@Override
	protected void shutDown()
	{
		started = false;
		rebuildAfterLogin = false;
		boolean restoreScene = tileRecolourMode;
		tileRecolourMode = false;
		nextReloadTick = -1;
		clientThread.invokeLater(() ->
		{
			tileRecolourer.restore();
			if (restoreScene && client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
		if (boundaryOverlay != null)
		{
			overlayManager.remove(boundaryOverlay);
			boundaryOverlay = null;
		}
		if (overlay != null)
		{
			overlayManager.remove(overlay);
			overlay = null;
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
			panel = null;
		}
		latestRegionId = OptionalInt.empty();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				// Only consume an explicitly deferred mode switch, never loop after a reload.
				if (started && rebuildAfterLogin && client.getGameState() == GameState.LOGGED_IN)
				{
					rebuildAfterLogin = false;
					tileRecolourer.restore();
					client.setGameState(GameState.LOADING);
				}
				refreshSceneState();
			});
		}
		else
		{
			if (boundaryOverlay != null)
			{
				boundaryOverlay.clear();
			}
			if (event.getGameState() != GameState.LOADING)
			{
				updateLatestRegionId(OptionalInt.empty());
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (AreaDarkenerConfig.GROUP.equals(event.getGroup()))
		{
			if ("boundaryLines".equals(event.getKey()))
			{
				clientThread.invokeLater(() -> { refreshSceneState(); });
			}
			else
			{
				requestReload();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (nextReloadTick >= 0 && client.getTickCount() >= nextReloadTick)
		{
			nextReloadTick = -1;
			tileRecolourer.restore();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		}
		if (boundaryOverlay != null)
		{
			boundaryOverlay.refresh();
		}
		if (panel != null && panel.isPanelShowing())
		{
			updateLatestRegionId();
		}
	}

	@Provides
	AreaDarkenerConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(AreaDarkenerConfig.class);
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

	int currentDarkness()
	{
		// Resolve from the player each rendered frame, including instance template coordinates.
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return 0;
		}
		WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		return point == null ? 0 : darknessForRegion(point.getRegionID()).orElse(0);
	}

	@Subscribe
	public void onPreMapLoad(PreMapLoad event)
	{
		if (tileRecolourMode)
		{
			tileRecolourer.recolor(event.getScene());
		}
	}

	boolean tileRecolourEnabled()
	{
		return tileRecolourMode;
	}

	void requestReload()
	{
		clientThread.invokeLater(() ->
		{
			if (!started)
			{
				return;
			}
			boolean wasTileMode = tileRecolourMode;
			tileRecolourMode = config.tileRecolour();
			if (wasTileMode != tileRecolourMode)
			{
				// Rebuild immediately when switching, so the two modes cannot overlap.
				nextReloadTick = -1;
				tileRecolourer.restore();
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					rebuildAfterLogin = false;
					client.setGameState(GameState.LOADING);
				}
				else if (client.getGameState() == GameState.LOADING)
				{
					// The current load may already have uploaded colors from the previous mode.
					rebuildAfterLogin = true;
				}
			}
			else if (tileRecolourMode)
			{
				// Coalesce config and side-panel changes into one client scene rebuild.
				nextReloadTick = client.getTickCount() + 1;
			}
			refreshSceneState();
		});
	}

	private void refreshSceneState()
	{
		updateLatestRegionId();
		if (boundaryOverlay != null)
		{
			boundaryOverlay.refresh();
		}
	}

	OptionalInt currentRegionId()
	{
		return latestRegionId;
	}

	private void updateLatestRegionId()
	{
		Player player = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || player == null)
		{
			updateLatestRegionId(OptionalInt.empty());
			return;
		}
		WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		updateLatestRegionId(point == null ? OptionalInt.empty() : OptionalInt.of(point.getRegionID()));
	}

	private void updateLatestRegionId(OptionalInt regionId)
	{
		if (latestRegionId.equals(regionId))
		{
			return;
		}
		latestRegionId = regionId;
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.rebuild();
			}
		});
	}
	private static BufferedImage createIcon()
	{
		try (InputStream inputStream = AreaDarkenerPlugin.class.getResourceAsStream("area-darkener-icon.png"))
		{
			if (inputStream != null)
			{
				return ImageIO.read(inputStream);
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to load Area Darkener icon", ex);
		}

		throw new IllegalStateException("Area Darkener icon resource is missing");
	}

}
