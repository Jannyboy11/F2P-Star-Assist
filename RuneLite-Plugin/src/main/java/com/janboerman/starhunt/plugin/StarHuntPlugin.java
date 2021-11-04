package com.janboerman.starhunt.plugin;

import javax.inject.Inject;
import javax.swing.*;

import com.google.inject.Provides;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.RunescapeUser;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarLocation;
import com.janboerman.starhunt.common.StarTier;

import com.janboerman.starhunt.common.lingo.StarLingo;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.World;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import okhttp3.Call;
import okio.Buffer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@PluginDescriptor(name = "F2P Star Hunt")
public class StarHuntPlugin extends Plugin {

	//populated manually
	private final StarCache starCache;

	//populated right after construction
	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private StarHuntConfig config;
	@Inject private WorldService worldService;
	@Inject private ClientThread clientThread;

	//populated on start-up
	private StarClient starClient;
	private StarHuntPanel panel;
	private NavigationButton navButton;

	public StarHuntPlugin() {
		this.starCache = new StarCache();
	}

	@Provides
	StarHuntConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(StarHuntConfig.class);
	}

	//TODO handle config reloads properly
	//TODO respect configuration settings

	@Override
	protected void startUp() throws Exception {
		this.starClient = injector.getInstance(StarClient.class);
		this.panel = new StarHuntPanel(this, config);
		BufferedImage icon = ImageUtil.loadImageResource(StarHuntPlugin.class, "/icon.png");
		this.navButton = NavigationButton.builder()
				.tooltip("F2P Star Hunt")
				.icon(icon)
				.priority(10)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		fetchStarList();

		log.info("F2P Star Hunt started!");
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(navButton);

		log.info("F2P Star Hunt stopped!");
	}

	public void fetchStarList() {
		if (config.httpConnectionEnabled()) {
			CompletableFuture<Set<CrashedStar>> starFuture = starClient.requestStars();
			starFuture.whenCompleteAsync((stars, ex) -> {
				if (ex != null) {
					log.error("Error when receiving star data", ex);
					return;
				}

				clientThread.invoke(() -> {
					starCache.addAll(stars);
					updatePanel();
				});

				String message = stars.isEmpty() ? "There are no known live stars currently." : "A handful of stars is alive!";
				log.debug(message);
			});
		}
	}


	public void hopAndHint(CrashedStar star) {
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
				});
			}
		}

		if (config.hintArrowEnabled()) {
			WorldPoint starPoint = StarPoints.fromLocation(star.getLocation());
			clientThread.invoke(() -> client.setHintArrow(starPoint));
		}
	}


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

	private boolean shouldBroadcastStar(StarKey starKey) {
		if (!config.httpConnectionEnabled()) return false;

		net.runelite.http.api.worlds.World w = worldService.getWorlds().findWorld(starKey.getWorld());
		boolean isPvP = w.getTypes().contains(net.runelite.http.api.worlds.WorldType.PVP);
		boolean isWilderness = starKey.getLocation().isInWilderness();
		return (config.sharePvpWorldStars() || !isPvP) && (config.shareWildernessStars() || !isWilderness);
	}

	public void reportStarNew(CrashedStar star, String broadcastToGroup) {
		log.debug("reporting new star: " + star);

		boolean isNew = starCache.add(star);
		if (isNew) {
			updatePanel();
		}

		if (broadcastToGroup != null && !broadcastToGroup.isEmpty() && shouldBroadcastStar(star.getKey())) {
			CompletableFuture<Optional<CrashedStar>> upToDateStar = starClient.sendStar(star);
			upToDateStar.whenCompleteAsync((optionalStar, ex) -> {
				if (ex != null) {
					logServerError(ex);
				} else if (optionalStar.isPresent()) {
					CrashedStar theStar = optionalStar.get();
					clientThread.invoke(() -> {
						starCache.forceAdd(theStar);
						updatePanel();
					});
				}
			});
		}
	}

	public void reportStarUpdate(StarKey starKey, StarTier newTier, boolean broadcast) {
		log.debug("reporting star update: " + starKey + "->" + newTier);

		CrashedStar star = starCache.get(starKey);
		if (star.getTier() == newTier) return;

		star.setTier(newTier);
		updatePanel();

		if (broadcast && shouldBroadcastStar(star.getKey())) {
			CompletableFuture<CrashedStar> upToDateStar = starClient.updateStar(star.getKey(), newTier);
			upToDateStar.whenCompleteAsync((theStar, ex) -> {
				if (ex != null) {
					logServerError(ex);
				}

				else if (!theStar.equals(star)) {
					clientThread.invoke(() -> { starCache.forceAdd(theStar); updatePanel(); });
				}
			});
		}
	}

	public void reportStarGone(StarKey starKey, boolean broadcast) {
		log.debug("reporting star gone: " + starKey);

		starCache.remove(starKey);
		updatePanel();

		if (broadcast && shouldBroadcastStar(starKey)) {
			starClient.deleteStar(starKey);
		}
	}

	private void logServerError(Throwable ex) {
		log.warn("Unexpected result from web server", ex);
		if (ex instanceof ResponseException) {
			Call call = ((ResponseException) ex).getCall();
			log.debug("Request that caused it: " + call.request());
			Buffer buffer = new Buffer();
			try {
				call.request().body().writeTo(buffer);
				log.debug("Request body: " + buffer.readString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				log.error("Error reading call request body", e);
			}
		}
	}

	private void updatePanel() {
		log.debug("Panel repaint!");
		assert client.isClientThread() : "updatePanel must be called from the client thread!";

		Set<CrashedStar> stars = new HashSet<>(starCache.getStars());
		SwingUtilities.invokeLater(() -> panel.setStars(stars));
	}


	//
	// ======= Event Listeners =======
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
						reportStarGone(star.getKey(), true);
						if (starPoint.equals(client.getHintArrowPoint())) {
							client.clearHintArrow();
						}
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
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			clientThread.invokeLater(() -> {
				for (CrashedStar star : starCache.getStars()) {
					if (star.getWorld() == client.getWorld()) {
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "A star has crashed in this world!", null);
						if (config.hintArrowEnabled()) {
							client.setHintArrow(StarPoints.fromLocation(star.getLocation()));
						}
						break;
					}
				}
			});
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		if (client.getGameState() != GameState.LOGGED_IN) return;	//player not in the world

		GameObject gameObject = event.getGameObject();
		StarTier starTier = StarIds.getTier(gameObject.getId());
		if (starTier == null) return;	//not a star

		WorldPoint worldPoint = gameObject.getWorldLocation();
		StarKey starKey = new StarKey(StarPoints.toLocation(worldPoint), client.getWorld());

		log.debug("A " + starTier + " star just despawned at location: " + worldPoint + ".");

		if (playerInStarRange(worldPoint)) {
			if (starTier == StarTier.SIZE_1) {
				//the star was mined out completely, or it poofed at t1.
				reportStarGone(starKey, true);
			} else {
				//it either degraded one tier, or disintegrated completely (poofed).
				//check whether a new star exists in the next game tick
				clientThread.invokeLater(() -> {
					if (client.getGameState() == GameState.LOGGED_IN && playerInStarRange(worldPoint)) {
						LocalPoint localStarPoint = LocalPoint.fromWorld(client, worldPoint);
						Tile tile = client.getScene().getTiles()[worldPoint.getPlane()][localStarPoint.getSceneX()][localStarPoint.getSceneY()];

						StarTier newTier = null;
						for (GameObject go : tile.getGameObjects()) {
							if (go != null) {
								StarTier tier = StarIds.getTier(go.getId());
								if (tier == starTier.oneLess()) {
									//a new star exists
									newTier = tier;
									break;
								}
							}
						}

						if (newTier == null) {
							//the star has poofed
							reportStarGone(starKey, true);
						} else {
							//the star has degraded
							reportStarUpdate(starKey, newTier, true);
						}
					}
				});
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starTier = StarIds.getTier(gameObject.getId());
		if (starTier == null) return;	//not a star

		WorldPoint worldPoint = gameObject.getWorldLocation();
		StarKey starKey = new StarKey(StarPoints.toLocation(worldPoint), client.getWorld());

		log.debug("A " + starTier + " star spawned at location: " + worldPoint + ".");

		CrashedStar knownStar = starCache.get(starKey);
		if (knownStar == null) {
			//we found a new star
			reportStarNew(new CrashedStar(starKey, starTier, Instant.now(), new RunescapeUser(client.getLocalPlayer().getName())), null /*TODO default group*/);
		}
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.

	private boolean isWorld(int world) {
		net.runelite.http.api.worlds.WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) return false;
		return worldResult.findWorld(world) != null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		String message = event.getMessage();

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
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(event.getName()));
						reportStarNew(star, config.shareCallsReceivedByFriendsChat());
					}
				}
				break;
			case CLAN_CHAT:
				if (config.interpretClanChat()) {
					//if player doesn't have group code, ignore TODO
					if ((tier = StarLingo.interpretTier(message)) != null
							&& (location = StarLingo.interpretLocation(message)) != null
							&& (world = StarLingo.interpretWorld(message)) != -1
							&& isWorld(world)) {
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(event.getName()));
						reportStarNew(star, config.shareCallsReceivedByClanChat());
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
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(event.getName()));
						reportStarNew(star, config.shareCallsReceivedByPrivateChat());
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
						CrashedStar star = new CrashedStar(tier, location, world, Instant.now(), new RunescapeUser(event.getName()));
						reportStarNew(star, config.shareCallsReceivedByPublicChat());
					}
				}
				break;
		}
	}
}
