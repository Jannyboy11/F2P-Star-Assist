package com.janboerman.starhunt.plugin;

import javax.inject.Inject;

import com.google.inject.Provides;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarTier;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private final StarCache localCache = new StarCache();

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
	// ======= Event Listeners =======
	//

	//TODO update knownStars accordingly.
	//TODO make sure not to send updates to the web server too frequently.
	//TODO if a star is despawned, check whether it poofed, or a player got out of sight (or logged out)
	//TODO 		we must ensure that we send degrade-updates or deletion-updates correctly.

	private int gameTickCounter = 0;
	private int spawnedTick = -1;
	private int despawnTick = -1;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			gameTickCounter= 0;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		gameTickCounter += 1;
	}


	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starSize = StarIds.getTier(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();
			spawnedTick = gameTickCounter;
			log.debug("gameTick = " + gameTickCounter);

			log.info("A " + starSize + " star spawned at location: " + worldPoint + ".");
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starSize = StarIds.getTier(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();
			despawnTick = gameTickCounter;
			log.debug("gameTick = "+ gameTickCounter);

			log.info("A " + starSize + " star just despawned at location: " + worldPoint + ".");
		}
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.









}

//TODO listen on friends chat message event (configurable),
//TODO listen to private chat message event (configurable),
//TODO listen to clan chat message event (configurable),
//TODO detect a CrashedStar instance from the message
