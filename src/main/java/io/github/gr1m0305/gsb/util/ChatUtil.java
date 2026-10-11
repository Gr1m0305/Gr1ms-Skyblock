package io.github.gr1m0305.gsb.util;

import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.gui.LegacyText;
import net.minecraft.client.Minecraft;

public final class ChatUtil {
	private static final String PREFIX = "§6[GSB] §r";

	private ChatUtil() {
	}

	/** Shows a message from this mod in the player's chat. The text can use formatting codes. */
	public static void send(String message) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player != null) {
			GameCompat.chat(minecraft).addClientSystemMessage(LegacyText.parse(PREFIX + message));
		}
	}
}
