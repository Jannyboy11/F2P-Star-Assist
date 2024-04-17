package com.janboerman.f2pstarassist.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("F2P Star Assist")
public interface StarAssistConfig extends Config {

	// ============================================ HTTP ============================================ \\
	//																							      \\

	@ConfigSection(
			name = "F2P StarHunt integration",
			description = "Settings for sending and receiving data from the F2P StarHunt webserver",
			position = 0,
			closedByDefault = true
	)
	public static final String HTTP_SETTINGS_SECTION = "HTTP Settings";

	//																							      \\
	//																								  \\

	@ConfigItem(
			position = 1,
			keyName = "http enabled",
			name = "Enable webserver communication",
			description = "Whether a connection with the webserver should be established",
			section = HTTP_SETTINGS_SECTION
	)
	default boolean httpConnectionEnabled() {
		return false;
	}

	@ConfigItem(
			position = 2,
			keyName = "address",
			name = "Webserver URL",
			description = "The address of the webserver with which star locations are shared",
			section = HTTP_SETTINGS_SECTION
	)
	default String httpUrl() {
		// TODO actually change the default url to the production url.
		return "http://localhost:8080";
	}

	//																								  \\
	// ============================================================================================== \\

	// ======================================== Chat Analysis ======================================= \\
	//																								  \\

	@ConfigSection(
			name = "Chat Analysis Settings",
			description = "Settings for whether to interpret star calls from chat messages",
			position = 3,
			closedByDefault = false
	)
	public static final String CHAT_SETTINGS_SECTION = "Chat Settings";

	//																								  \\
	//																								  \\

	@ConfigItem(
			position = 4,
			keyName = "clan chat",
			name = "Interpret clan chat star calls",
			description = "Check whether clan chat messages contain star calls",
			section = CHAT_SETTINGS_SECTION
	)
	default boolean interpretClanChat() {
		return true;
	}

	@ConfigItem(
			position = 5,
			keyName = "friends chat",
			name = "Interpret friends chat star calls",
			description = "Check whether friends chat messages contain star calls",
			section = CHAT_SETTINGS_SECTION
	)
	default boolean interpretFriendsChat() {
		return true;
	}

	@ConfigItem(
			position = 6,
			keyName = "private chat",
			name = "Interpret private chat star calls",
			description = "Check whether private chat messages contain star calls",
			section = CHAT_SETTINGS_SECTION
	)
	default boolean interpretPrivateChat() {
		return true;
	}

	@ConfigItem(
			position = 7,
			keyName = "public chat",
			name = "Interpret public chat star calls",
			description = "Check whether public chat messages contain star calls",
			section = CHAT_SETTINGS_SECTION
	)
	default boolean interpretPublicChat() {
		return true;
	}

	//																								  \\
	// ============================================================================================== \\


	// ========================================= Hint Arrow ========================================= \\
	//																								  \\

	@ConfigSection(
			name = "Miscellaneous Settings",
			description = "Settings that don't belong in any other category",
			position = 8,
			closedByDefault = false
	)
	public static final String MISCELLANEOUS = "Miscellaneous Settings";

	//																								  \\
	//																								  \\

	@ConfigItem(
			position = 9,
			keyName = "hint enabled",
			name = "Enable arrow hints",
			description = "Whether to display an arrow that hints to the target location",
			section = MISCELLANEOUS
	)
	default boolean hintArrowEnabled() {
		return false;
	}

	@ConfigItem(
			position = 10,
			keyName = "double hopping locations",
			name = "Mark double hopping location tiles",
			description = "Enable tile markers for double hopping locations.<br>" +
					"F2P Double Hopping spots:<br>" +
					"- Between Crafting Guild and Rimmington mine<br>" +
					"- Between Desert mine and PvP Arena<br>" +
					"- Between Aubury and Varrock east mine (1 tile diff)<br>" +
					"- Between Lumbridge Swamp south east and Al Kharid bank (3 tile diff)",
			section = MISCELLANEOUS
	)
	default boolean markDoubleHoppingTiles() {
		return false;
	}

	//																								  \\
	// ============================================================================================== \\

}
