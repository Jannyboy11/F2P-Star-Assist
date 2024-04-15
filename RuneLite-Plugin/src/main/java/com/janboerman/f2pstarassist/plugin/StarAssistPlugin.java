package com.janboerman.f2pstarassist.plugin;

import com.google.inject.Provides;
import com.janboerman.f2pstarassist.plugin.lingo.StarLingo;
import com.janboerman.f2pstarassist.plugin.model.CrashedStar;
import com.janboerman.f2pstarassist.plugin.model.DeletionMethod;
import com.janboerman.f2pstarassist.plugin.model.RunescapeUser;
import com.janboerman.f2pstarassist.plugin.model.StarKey;
import com.janboerman.f2pstarassist.plugin.model.StarLocation;
import com.janboerman.f2pstarassist.plugin.model.StarTier;
import com.janboerman.f2pstarassist.plugin.ui.DoubleHoppingTilesOverlay;
import com.janboerman.f2pstarassist.plugin.ui.StarAssistPanel;
import com.janboerman.f2pstarassist.plugin.web.ResponseException;

import static com.janboerman.f2pstarassist.plugin.TextUtil.stripChatIcon;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;

import okhttp3.Call;
import okhttp3.RequestBody;
import okio.Buffer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

@Slf4j
@PluginDescriptor(name = "F2P Star Assist")
public class StarAssistPlugin extends Plugin {

	//populated at construction
	private final StarCache starCache;

	//populated right after construction
	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private WorldService worldService;
	@Inject private ClientThread clientThread;
	@Inject private StarAssistConfig config;
	@Inject private OverlayManager overlayManager;

	//populated on start-up
	private StarClient starClient;
	private DoubleHoppingTilesOverlay doubleHoppingTilesOverlay;
	private ScheduledExecutorService fetcherTimer;
	private StarAssistPanel panel;
	private NavigationButton navButton;

	public StarAssistPlugin() {
		this.starCache = new StarCache(removalNotification -> {
			if (removalNotification.wasEvicted()) { //'evicted' meaning: not explicitly removed by a 'StarCache#remove' call.
				//remove from sidebar.
				clientThread.invokeLater(this::updatePanel);
			}

			clientThread.invoke(() -> {
				CrashedStar removedStar = removalNotification.getValue();

				//if a hint arrow pointing to the removed star exists, then clear it.
				if (removedStar.getWorld() == client.getWorld()) {
					WorldPoint starPoint = StarPoints.fromLocation(removedStar.getLocation());
					if (client.hasHintArrow() && client.getHintArrowPoint().equals(starPoint)) {
						client.clearHintArrow();
					}
				}
			});
		});
	}

	@Provides
	StarAssistConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(StarAssistConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		this.starClient = injector.getInstance(StarClient.class);
		this.doubleHoppingTilesOverlay= injector.getInstance(DoubleHoppingTilesOverlay.class);
		overlayManager.add(doubleHoppingTilesOverlay);

		this.panel = new StarAssistPanel(this, config, client, clientThread);
		BufferedImage icon = ImageUtil.loadImageResource(StarAssistPlugin.class, "/icon.png");
		this.navButton = NavigationButton.builder()
				.tooltip("F2P Star Assist")
				.icon(icon)
				.priority(10)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		fetcherTimer = Executors.newSingleThreadScheduledExecutor();

		fetcherTimer.scheduleAtFixedRate(() -> {
			clientThread.invoke(this::fetchStarList);
		}, 0, 5, TimeUnit.MINUTES);

		clientThread.invoke(this::updatePanel);

		log.info("F2P Star Assist started!");
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(doubleHoppingTilesOverlay);
		clientToolbar.removeNavigation(navButton);

		fetcherTimer.shutdownNow();
		fetcherTimer = null;

		starCache.clear();

		log.info("F2P Star Assist stopped!");
	}

	// TODO do we want this method in this class?
	public void fetchStarList() {
		if (config.httpConnectionEnabled()) {
			CompletableFuture<List<CrashedStar>> starFuture = starClient.requestStars();
			starFuture.whenCompleteAsync((receivedStars, ex) -> {
				if (ex != null) {
					log.error("Error when receiving star data", ex);
				} else {
					log.debug("received stars from webserver: " + receivedStars);

					clientThread.invoke(() -> receiveStars(receivedStars));
				}
			});
		}
	}

	public void hopAndHint(CrashedStar star) {
		assert !client.isClientThread();

		int starWorld = star.getWorld();
		int currentWorld = client.getWorld();

		if (currentWorld != starWorld) {
			net.runelite.http.api.worlds.WorldResult worldResult = worldService.getWorlds();
			if (worldResult != null) {
				clientThread.invoke(() -> {
					World world = rsWorld(worldResult.findWorld(starWorld));
					if (world != null) {
						client.hopToWorld(world);
					}

					if (config.hintArrowEnabled()) {
						WorldPoint starPoint = StarPoints.fromLocation(star.getLocation());
						client.setHintArrow(starPoint);
					}
				});
			}
		}
	}

	public void showHintArrow(boolean whetherTo) {
		assert client.isClientThread();

		int playerWorld = client.getWorld();
		// TODO: optimise StarCache, store only by world?
		for (CrashedStar star : starCache.getStars()) {
			if (star.getWorld() == playerWorld) {
				WorldPoint starPoint = StarPoints.fromLocation(star.getLocation());
				if (whetherTo) {
					client.setHintArrow(starPoint);
				} else if (client.hasHintArrow() && client.getHintArrowPoint().equals(starPoint)) {
					client.clearHintArrow();
				}
				break;
			}
		}
	}

	@Nullable
	private World rsWorld(net.runelite.http.api.worlds.World world) {
		if (world == null) return null;
		assert client.isClientThread();

		World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		return rsWorld;
	}

	//
	// ======= Star Cache Bookkeeping =======
	//

	public void receiveStars(List<CrashedStar> starList) {
		starCache.receiveStars(starList);

		updatePanel();
	}

	private boolean shouldBroadcast(StarSource methodFound) {
		return config.httpConnectionEnabled() && methodFound == StarSource.FOUND_IN_GAME;
	}

	public void reportStarNew(CrashedStar star, StarSource methodFound) {
		assert client.isClientThread();

		if (!isF2pWorld(star.getWorld())) {
			log.debug("received non-f2p star: " + star);
			return;
		}

		log.debug("reporting new star: " + star);

		final boolean isNew = starCache.add(star) == null;
		if (isNew) {
			updatePanel();
		}

		if (shouldBroadcast(methodFound)) {
			CompletableFuture<Long> upToDateStar = starClient.postStar(star);
			upToDateStar.whenCompleteAsync((databaseId, ex) -> {
				if (ex != null) {
					logServerError(ex);
				} else if (databaseId != null) {
					clientThread.invoke(() -> star.setId(databaseId.longValue()));
				}
			});
		}
	}

	public void reportStarUpdate(StarKey starKey, StarTier newTier, StarSource methodFound) {
		assert client.isClientThread();

		log.debug("reporting star update: " + starKey + "->" + newTier);

		CrashedStar star = starCache.get(starKey);
		if (star.getTier() == newTier) return;

		star.setTier(newTier);
		updatePanel();

		if (shouldBroadcast(methodFound)) {
			CompletableFuture<Void> upToDateStar = starClient.updateStarTier(starKey, newTier);
			upToDateStar.whenCompleteAsync((receivedStar, ex) -> {
				if (ex != null) {
					logServerError(ex);
				}
			});
		}
	}

	public void reportStarGone(StarKey starKey, DeletionMethod deletionMethod, StarSource methodFound) {
		assert client.isClientThread();

		log.debug("reporting star gone: " + starKey);

		starCache.remove(starKey);
		updatePanel();

		if (shouldBroadcast(methodFound)) {
			CompletableFuture<Void> deleteAction = starClient.deleteStar(starKey, deletionMethod);
			deleteAction.whenComplete((Void v, Throwable ex) -> {
				if (ex != null) {
					logServerError(ex);
				}
				else {
					log.debug("star " + starKey + " deleted from server");
				}
			});
		}
	}

	public void removeStar(StarKey starKey) {
		assert client.isClientThread();

		starCache.remove(starKey);
	}

	private void logServerError(Throwable ex) {
		log.warn("Unexpected result from web server", ex);
		if (ex instanceof ResponseException) {
			Call call = ((ResponseException) ex).getCall();
			log.debug("Request that caused it: " + call.request());
			Buffer buffer = new Buffer();
			try {
				RequestBody body = call.request().body();
				if (body != null) {
					body.writeTo(buffer);
					log.debug("Request body: " + buffer.readString(StandardCharsets.UTF_8));
				} else {
					log.debug("Request body: <empty>");
				}
			} catch (IOException e) {
				log.error("Error reading call request body", e);
			}
		}
	}

	@Nullable
	public WorldPoint getLocalPlayerLocation() {
		assert client.isClientThread() : "getLocalPlayerLocation must be called from the client thread!";

		Player localPlayer = client.getLocalPlayer();
		return localPlayer == null ? null : localPlayer.getWorldLocation();
	}

	private void updatePanel() {
		log.debug("Panel repaint!");
		assert client.isClientThread() : "updatePanel must be called from the client thread!";

		Set<CrashedStar> stars = new HashSet<>(starCache.getStars());
		WorldPoint playerLocation = getLocalPlayerLocation();

		SwingUtilities.invokeLater(() -> panel.setStars(stars, playerLocation));
	}


	//
	// ======= Helper methods =======
	//

	private static final int DETECTION_DISTANCE = 25;

	private boolean playerInStarRange(WorldPoint starLocation) {
		return playerInRange(starLocation, DETECTION_DISTANCE);
	}

	private boolean playerInRange(WorldPoint worldPoint, int distance) {
		return inManhattanRange(client.getLocalPlayer().getWorldLocation(), worldPoint, distance);
	}

	private static boolean inManhattanRange(WorldPoint playerLoc, WorldPoint targetLoc, int distance) {
		int playerX = playerLoc.getX();
		int playerY = playerLoc.getY();
		int starX = targetLoc.getX();
		int starY = targetLoc.getY();
		return playerLoc.getPlane() == targetLoc.getPlane()
				&& starX - distance <= playerX && playerX <= starX + distance
				&& starY - distance <= playerY && playerY <= starY + distance;
	}

	private static StarTier getStar(Tile tile) {
		for (GameObject gameObject : tile.getGameObjects()) {
			if (gameObject != null) {
				StarTier starTier = StarIds.getTier(gameObject.getId());
				if (starTier != null) return starTier;
			}
		}
		return null;
	}

	private boolean isWorld(int world) {
		net.runelite.http.api.worlds.WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) return false;
		return worldResult.findWorld(world) != null;
	}

	private boolean isF2pWorld(int world) {
		net.runelite.http.api.worlds.WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) return true; //world-service is broken, assume f2p world.
		net.runelite.http.api.worlds.World apiWorld = worldResult.findWorld(world);
		if (apiWorld == null) return false;
		return !apiWorld.getTypes().contains(net.runelite.http.api.worlds.WorldType.MEMBERS);
	}

	// TODO how to get the health of the star? look at the star info plugin.
	private static DeletionMethod deletionMethod(StarTier tier, float health) {
		if (tier == StarTier.SIZE_1 && health < 0.1F) {
			return DeletionMethod.DEPLETED;
		} else {
			return DeletionMethod.DISINTEGRATED;
		}
	}


	//
	// ======= Event Listeners =======
	//

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if ("F2P Star Assist".equals(event.getGroup())) {
			if ("hint enabled".equals(event.getKey())) {
				showHintArrow(Boolean.parseBoolean(event.getNewValue()));
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			showHintArrow(config.hintArrowEnabled());
		}
	}

	@Subscribe
	public void onWorldChanged(WorldChanged event) {
		showHintArrow(config.hintArrowEnabled());
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		for (CrashedStar star : starCache.getStars()) {
			if (client.getWorld() == star.getWorld()) {
				WorldPoint starPoint = StarPoints.fromLocation(star.getLocation());
				if (starPoint != null && playerInStarRange(starPoint)) {
					LocalPoint localPoint = LocalPoint.fromWorld(client, starPoint);
					Tile tile = client.getScene().getTiles()[starPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];

					StarTier starTier = getStar(tile);
					if (starTier == null) {
						//a star that was in the cache is no longer there.
						clientThread.invokeLater(() -> {
							if (getStar(tile) == null) {
								//if in the next tick there is still no star, report it as gone.
								reportStarGone(star.getKey(), deletionMethod(star.getTier(), star.getHealth()), StarSource.FOUND_IN_GAME);
								if (starPoint.equals(client.getHintArrowPoint())) {
									client.clearHintArrow();
								}
							}
						});
					}

					else if (playerInRange(starPoint, 4) && starPoint.equals(client.getHintArrowPoint())) {
						//if the player got withing a range of 4, clear the arrow.
						client.clearHintArrow();
					}
				}
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starTier = StarIds.getTier(gameObject.getId());
		if (starTier == null) return;	//not a star

		WorldPoint worldPoint = gameObject.getWorldLocation();
		StarLocation starLocation = StarPoints.toLocation(worldPoint);
		if (starLocation == null) {
			log.error("Unrecognised star location at world point: " + worldPoint);
			return;
		}
		StarKey starKey = new StarKey(starLocation, client.getWorld());

		log.debug("A " + starTier + " star spawned at location: " + worldPoint + ".");

		CrashedStar knownStar = starCache.get(starKey);
		if (knownStar == null) {
			//we found a new star
			CrashedStar newStar = new CrashedStar(starKey, starTier, Instant.now(), new RunescapeUser(client.getLocalPlayer().getName()));
			reportStarNew(newStar, StarSource.FOUND_IN_GAME);
		} else {
			//the star already exists.
			StarTier upToDateTier = StarIds.getTier(gameObject.getId());
			if (upToDateTier != null) {
				reportStarUpdate(starKey, upToDateTier, StarSource.FOUND_IN_GAME);
			}
		}

		//show hint arrow
		if (config.hintArrowEnabled() && !client.hasHintArrow()) {
			client.setHintArrow(worldPoint);
		}
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.
	// We don't listen on GameObjectDespawned, because onGameTick already handles disintegrated stars.


	// Use NPC events to determine the health of the star.

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() != StarIds.NULL_NPC_ID) return;	// not the star health bar

		WorldPoint worldPoint = npc.getWorldLocation();
		StarLocation starLocation = StarPoints.toLocation(worldPoint);
		if (starLocation == null) return; // not a star

		StarKey starKey = new StarKey(starLocation, client.getWorld());

		// Look up the star in the next tick (should the NpcSpawned event be called earlier than GameObjectSpawned event).
		clientThread.invoke(() -> {
			CrashedStar star = starCache.get(starKey);
			if (star == null) return;
			star.setHealthNpc(npc);
		});
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() != StarIds.NULL_NPC_ID) return; // not the star health bar

		WorldPoint worldPoint = npc.getWorldLocation();
		StarLocation starLocation = StarPoints.toLocation(worldPoint);
		if (starLocation == null) return; //not a star

		StarKey starKey = new StarKey(starLocation, client.getWorld());
		CrashedStar star = starCache.get(starKey);
		if (star == null) return;

		star.setHealthNpc(null);
	}

	// Capture star calls from chat messages.

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		final String message = event.getMessage();

		StarLocation location;
		int world;
		StarTier tier;

		switch (event.getType()) {
			case FRIENDSCHAT:
				if (config.interpretFriendsChat()) {
					if ((tier = StarLingo.interpretTier(message)) != null
							&& (location = StarLingo.interpretLocation(message)) != null
							&& (world = StarLingo.interpretWorld(message)) != -1
							&& isWorld(world)) {
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(stripChatIcon(event.getName())));
						reportStarNew(star, StarSource.FRIENDS_CHAT);
					}
				}
				break;
			case CLAN_CHAT:
				if (config.interpretClanChat()) {
					if ((tier = StarLingo.interpretTier(message)) != null
							&& (location = StarLingo.interpretLocation(message)) != null
							&& (world = StarLingo.interpretWorld(message)) != -1
							&& isWorld(world)) {
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(stripChatIcon(event.getName())));
						reportStarNew(star, StarSource.CLAN_CHAT);
					}
				}
				break;
			case PRIVATECHAT:
			case MODPRIVATECHAT:
				if (config.interpretPrivateChat()) {
					if ((tier = StarLingo.interpretTier(message)) != null
							&& (location = StarLingo.interpretLocation(message)) != null
							&& (world = StarLingo.interpretWorld(message)) != -1
							&& isWorld(world)) {
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(stripChatIcon(event.getName())));
						reportStarNew(star, StarSource.PRIVATE_CHAT);
					}
				}
				break;
			case PUBLICCHAT:
			case MODCHAT:
				if (config.interpretPublicChat()) {
					if ((tier = StarLingo.interpretTier(message)) != null
							&& (location = StarLingo.interpretLocation(message)) != null
							&& (world = StarLingo.interpretWorld(message)) != -1
							&& isWorld(world)) {
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(stripChatIcon(event.getName())));
						reportStarNew(star, StarSource.PUBLIC_CHAT);
					}
				}
				break;
		}
	}

}
