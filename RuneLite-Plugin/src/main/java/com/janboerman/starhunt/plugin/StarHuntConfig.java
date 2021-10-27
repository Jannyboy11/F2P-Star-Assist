package com.janboerman.starhunt.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("F2P StarHunt")
public interface StarHuntConfig extends Config
{

	@ConfigSection(
			name = "HTTP Settings",
			description = "Settings for sending and receiving data from the webserver",
			position = 0,
			closedByDefault = false
	)
	public static final String HTTP_SETTINGS_SECTION = "HTTP Settings";

	@ConfigItem(
			position = 0,
			keyName = "send and receive",
			name = "Enable webserver communication",
			description = "Whether to send and receive data from the webserver.",
			section = HTTP_SETTINGS_SECTION
	)
	default boolean httpConnectionEnabled() {
		return true;
	}

	@ConfigItem(
			position = 1,
			keyName = "url",
			name = "Discord Bot Endpoint",
			description = "The address of the webserver with which star locations are shared.",
			section = HTTP_SETTINGS_SECTION
	)
	default String httpUrl() {
		return "http://localhost:8080";
	}

	//TODO hint arrow enabled?
	//TODO enabled in other worlds?
}
