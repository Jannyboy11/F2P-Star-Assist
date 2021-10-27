package com.janboerman.starhunt;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
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

	@Inject
	private StarHuntConfig config;

	@Provides
	StarHuntConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StarHuntConfig.class);
	}

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

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "F2P Star Hunt says " + config.greeting(), null);

			
		}
	}

	//
	// ======= Event Listeners =======
	//

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		StarSize starSize = StarSize.byObjectId(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();

			log.info("A " + starSize + " star spawned at location: (" + worldPoint.getX() + ", " + worldPoint.getY() + ").");
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject gameObject = event.getGameObject();
		StarSize starSize = StarSize.byObjectId(gameObject.getId());

		if (starSize != null) {
			WorldPoint worldPoint = gameObject.getWorldLocation();

			log.info("A " + starSize + " star just despawned at location: (" + worldPoint.getX() + ", " + worldPoint.getY() + ").");
		}
	}

	//If stars downscale, they just despawn and spawn a new one at a lower tier. GameObjectChanged is never called.

}
