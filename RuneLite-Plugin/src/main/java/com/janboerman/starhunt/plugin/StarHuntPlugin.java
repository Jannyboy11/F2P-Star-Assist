package com.janboerman.starhunt.plugin;

import javax.inject.Inject;

import com.janboerman.starhunt.common.StarTier;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "F2P Star Hunt"
)
public class StarHuntPlugin extends Plugin
{
	@Inject
	private Client client;

//	@Inject
//	private StarHuntConfig config;
//
//	@Provides
//	StarHuntConfig provideConfig(ConfigManager configManager)
//	{
//		return configManager.getConfig(StarHuntConfig.class);
//	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("F2P Star Hunt started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("F2P Star Hunt stopped!");
	}

//	@Subscribe
//	public void onGameStateChanged(GameStateChanged gameStateChanged)
//	{
//		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
//		{
//			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "F2P Star Hunt says " + config.greeting(), null);
//
//
//		}
//	}

	//
	// ======= Event Listeners =======
	//

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starSize = StarIds.getTier(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();

			log.info("A " + starSize + " star spawned at location: " + worldPoint + ".");
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject gameObject = event.getGameObject();
		StarTier starSize = StarIds.getTier(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();

			log.info("A " + starSize + " star just despawned at location: " + worldPoint + ".");
		}
	}

	// If stars degrade, they just de-spawn and spawn a new one at a lower tier. The GameObjectChanged event is never called.

}
