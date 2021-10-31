package com.janboerman.starhunt.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("F2P Star Hunt")
public interface StarHuntConfig extends Config {

	// ========================================= HTTP =========================================	\\
	//																							\\

	@ConfigSection(
			name = "Webserver Settings",
			description = "Settings for sending and receiving data from the webserver",
			position = 0,
			closedByDefault = false
	)
	public static final String HTTP_SETTINGS_SECTION = "HTTP Settings";

	//																							\\
	//																							\\

	@ConfigItem(
			position = 0,
			keyName = "send and receive",
			name = "Enable webserver communication",
			description = "Whether to send and receive data from the webserver",
			section = HTTP_SETTINGS_SECTION
	)
	default boolean httpConnectionEnabled() {
		return false;
	}

	@ConfigItem(
			position = 1,
			keyName = "url",
			name = "Webserver URL",
			description = "The address of the webserver with which star locations are shared",
			section = HTTP_SETTINGS_SECTION
	)
	default String httpUrl() {
		return "http://localhost:8080";
	}

	@ConfigItem(
			position = 2,
			keyName = "share pvp-world stars",
			name = "Share PVP-world stars",
			description = "Whether to send stars in PVP-worlds",
			section = HTTP_SETTINGS_SECTION
	)
	default boolean sharePvpWorldStars() {
		return false;
	}

	@ConfigItem(
			position = 3,
			keyName = "share wilderness stars",
			name = "Share Wilderness stars",
			description = "Whether to send stars in the Wilderness",
			section = HTTP_SETTINGS_SECTION
	)
	default boolean shareWildernessStars() {
		return false;
	}

	//																							\\
	// ======================================================================================== \\


	// ====================================== Hint Arrow ====================================== \\
	//																							\\

	@ConfigSection(
			name = "Hint Arrow Settings",
			description = "Settings for hint arrows",
			position = 1,
			closedByDefault = false
	)
	public static final String HINT_ARROW = "Hint Arrow Settings";

	//																							\\
	//																							\\

	@ConfigItem(
			position = 4,
			keyName = "hint enabled",
			name = "Enable map hints",
			description = "Whether to display an arrow that hints to the target location",
			section = HINT_ARROW
	)
	default boolean hintArrowEnabled() {
		return false;
	}

	//																							\\
	// ========================================================================================	\\


	// =================================== Chat Integration ===================================	\\
	//																							\\

	@ConfigSection(
			name = "Chat Integration Settings",
			description = "Integration with chat channels",
			position = 2,
			closedByDefault = false
	)
	public static final String CHAT_INTEGRATION = "Chat Integration Settings";

	//																							\\
	//																							\\

	@ConfigItem(
			position = 5,
			keyName = "friends chat",
			name = "Enable friends chat integration",
			description = "Analyze friends chat for possible star calls",
			section = CHAT_INTEGRATION
	)
	default boolean interpretFriendsChat() {
		return false;
	}

	@ConfigItem(
			position = 6,
			keyName = "private chat",
			name = "Enable private chat integration",
			description = "Analyze private chat for possible star calls",
			section = CHAT_INTEGRATION
	)
	default boolean interpretPrivateChat() {
		return false;
	}

	@ConfigItem(
			position = 7,
			keyName = "clan chat",
			name = "Enable clan chat integration",
			description = "Analyze clan chat for possible star calls",
			section = CHAT_INTEGRATION
	)
	default boolean interpretClanChat() {
		return false;
	}

	@ConfigItem(
			position = 8,
			keyName = "public chat",
			name = "Enable public chat integration",
			description = "Analyze public chat for possible star calls",
			section = CHAT_INTEGRATION
	)
	default boolean interpretPublicChat() {
		return false;
	}

	//																							\\
	// ======================================================================================== \\

	//TODO tile markers for star landing sites
	//TODO tile markers for hopping locations for (duel arena, al kharid mine) and (rimmington mine, crafting guild)

}
