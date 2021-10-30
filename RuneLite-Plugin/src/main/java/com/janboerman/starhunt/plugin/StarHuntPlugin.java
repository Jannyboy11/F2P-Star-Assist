package com.janboerman.starhunt.plugin;

import javax.inject.Inject;
import javax.swing.*;

import com.google.inject.Provides;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.RunescapeUser;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarTier;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldResult;
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

		updateStarList();

		log.info("F2P Star Hunt started!");
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(navButton);

		log.info("F2P Star Hunt stopped!");
	}

	public void updateStarList() {
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
			WorldResult worldResult = worldService.getWorlds();
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
			//TODO this int arrow should be cleared once the player arrives at the star
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

	public void reportStarNew(CrashedStar star, boolean broadcast) {
		log.debug("Found new star!");
		boolean isNew = starCache.add(star);
		if (isNew) {
			log.debug("Found a new star: " + star + "! Updating panel...");
			updatePanel();
		}

		if (broadcast && config.httpConnectionEnabled()) {
			CompletableFuture<Optional<CrashedStar>> upToDateStar = starClient.sendStar(star);
			upToDateStar.whenCompleteAsync((optionalStar, ex) -> {
				if (ex != null) {
					logServerError(ex);
				}

				else if (optionalStar.isPresent()) {
					CrashedStar theStar = optionalStar.get();
					clientThread.invoke(() -> { starCache.forceAdd(theStar); updatePanel(); });
				}
			});
		}
	}

	public void reportStarUpdate(CrashedStar star, StarTier newTier, boolean broadcast) {
		if (star.getTier() == newTier) return;

		star.setTier(newTier);
		updatePanel();

		if (broadcast && config.httpConnectionEnabled()) {
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
		starCache.remove(starKey);
		updatePanel();

		if (broadcast && config.httpConnectionEnabled()) {
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

	private boolean playerInRange(WorldPoint starLocation) {
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		int playerX = playerLocation.getX();
		int playerY = playerLocation.getY();
		int starX = starLocation.getX();
		int starY = starLocation.getY();
		return playerLocation.getPlane() == starLocation.getPlane()
				&& starX - DETECTION_DISTANCE <= playerX && playerX <= starX + DETECTION_DISTANCE
				&& starY - DETECTION_DISTANCE <= playerY && playerY <= starY + DETECTION_DISTANCE;
	}

	private int gameTick = 0;
	private StarKey despawnStarKey = null;
	private StarTier despawnStarTier = null;
	private int despawnStarTick = -1;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			gameTick = 0;

			//TODO check whether we are in a world in which a star exists,
			//TODO if so, display a message in chat! can we make this message clickable and display the hint arrow?
			//TODO and, if configured, display a hint arrow
		}
	}

	//called AFTER all packets have processed (so, after GameObjectDespawned and GameObjectSpawned I suppose)
	@Subscribe
	public void onGameTick(GameTick event) {
		if (despawnStarKey != null && despawnStarKey.getWorld() == client.getWorld() && gameTick == despawnStarTick) {
			assert despawnStarTier != null : "despawnStarKey is not null, but despawnStarTier is!";
			assert despawnStarTier.compareTo(StarTier.SIZE_1) > 0 : "despawnStarTier must never be never size 1!";

			WorldPoint starPoint = StarPoints.fromLocation(despawnStarKey.getLocation());
			Tile tile = client.getScene().getTiles()[starPoint.getPlane()][starPoint.getX()][starPoint.getY()];

			StarTier newTier = null;
			for (GameObject gameObject : tile.getGameObjects()) {
				StarTier tier = StarIds.getTier(gameObject.getId());
				if (tier == despawnStarTier.oneLess()) {
					//a new star exists
					newTier = tier;
					break;
				}
			}

			if (newTier == null) {
				reportStarGone(despawnStarKey, true);
			}

			despawnStarKey = null;
			despawnStarTier = null;
			despawnStarTick = -1;
		}

		gameTick += 1;	//This can't ever overflow. One would have to play for 113 years straight!
		assert gameTick > 0 : "Impossibru!";
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

		if (playerInRange(worldPoint)) {
			if (starTier == StarTier.SIZE_1) {
				//the star was mined out completely, or it poofed at t1.
				reportStarGone(starKey, true);
			} else {
				//it either degraded one tier, or disintegrated completely (poofed).
				//check whether a new star exists in onGameTick.
				despawnStarKey = starKey;
				despawnStarTier = starTier;
				despawnStarTick = gameTick;
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
			reportStarNew(new CrashedStar(starKey, starTier, Instant.now(), new RunescapeUser(client.getUsername())), true);
		} else {
			//a star has degraded
			reportStarUpdate(knownStar, starTier, true);
		}
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.

}

//TODO listen on friends chat message event (configurable),
//TODO listen to private chat message event (configurable),
//TODO listen to clan chat message event (configurable),
//TODO detect a CrashedStar instance from the message
