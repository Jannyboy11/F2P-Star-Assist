package com.janboerman.starhunt.plugin;

import javax.inject.Inject;

import com.google.inject.Provides;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarLocation;
import com.janboerman.starhunt.common.StarTier;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@PluginDescriptor(
	name = "F2P Star Hunt"
)
public class StarHuntPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private StarHuntConfig config;

	private StarClient starClient;
	private final StarCache starCache = new StarCache();

	@Provides
	StarHuntConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StarHuntConfig.class);
	}

	//TODO handle config reloads properly
	//TODO respect configuration settings

	@Override
	protected void startUp() throws Exception
	{
		this.starClient = injector.getInstance(StarClient.class);
		if (config.httpConnectionEnabled()) {
			CompletableFuture<Set<CrashedStar>> starFuture = starClient.requestStars();
			starFuture.whenCompleteAsync((stars, ex) -> {
				//TODO chat message? sidebar panel update is better I think? but also harder.
				//TODO let's just do a chat message first.
			});
		}

		log.info("F2P Star Hunt started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("F2P Star Hunt stopped!");
	}


	//
	// ======= Star Cache Bookkeeping =======
	// TODO make sure to post updates to the web server, if configured

	public void reportStarNew(CrashedStar star) {
		starCache.add(star);
	}

	public void reportStarUpdate(CrashedStar star, StarTier newTier) {
		star.setTier(newTier);
	}

	public void reportStarGone(StarKey starKey) {
		starCache.remove(starKey);
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
	public void onGameStateChange(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			gameTick = 0;
		}
	}

	//called AFTER all packets have processed (so, after GameObjectDespawned and GameObjectSpawned I suppose)
	@Subscribe
	public void onGameTick(GameTick event) {
		if (despawnStarKey != null && despawnStarKey.getWorld() == client.getWorld() && gameTick == despawnStarTick) {
			assert despawnStarTier != null : "despawnStarKey is not null, but despawnStarTier is!";

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
				reportStarGone(despawnStarKey);
			}

			despawnStarKey = null;
			despawnStarTier = null;
			despawnStarTick = -1;
		}

		gameTick += 1;
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		if (client.getGameState() != GameState.LOGGED_IN) return;	//player not in the world

		GameObject gameObject = event.getGameObject();
		StarTier starTier = StarIds.getTier(gameObject.getId());
		if (starTier == null) return;	//not a star

		WorldPoint worldPoint = gameObject.getWorldLocation();
		StarKey starKey = new StarKey(StarPoints.toLocation(worldPoint), client.getWorld());

		if (playerInRange(worldPoint)) {
			if (starTier == StarTier.SIZE_1) {
				//the star was mined out completely, or it poofed at t1.
				reportStarGone(starKey);
			} else {
				//it either degraded one tier, or disintegrated completely (poofed).
				//check whether a new star exists in onGameTick.
				despawnStarKey = starKey;
				despawnStarTier = starTier;
				despawnStarTick = gameTick;
			}
		}

		log.debug("A " + starTier + " star just despawned at location: " + worldPoint + ".");
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starTier = StarIds.getTier(gameObject.getId());
		if (starTier == null) return;	//not a star

		WorldPoint worldPoint = gameObject.getWorldLocation();
		StarKey starKey = new StarKey(StarPoints.toLocation(worldPoint), client.getWorld());

		CrashedStar knownStar = starCache.get(starKey);
		if (knownStar == null) {
			//we found a new star
			reportStarNew(new CrashedStar(starKey, starTier, Instant.now(), client.getUsername()));
		} else {
			//a star has degraded
			reportStarUpdate(knownStar, starTier);
		}

		log.debug("A " + starTier + " star spawned at location: " + worldPoint + ".");
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.

}

//TODO listen on friends chat message event (configurable),
//TODO listen to private chat message event (configurable),
//TODO listen to clan chat message event (configurable),
//TODO detect a CrashedStar instance from the message
