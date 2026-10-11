package io.github.gr1m0305.gsb.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The parts of Minecraft that are not the same in every version the mod is built for.
 * This is the one for Minecraft 26.1, every folder in src/compat has its own.
 */
public final class GameCompat {
	private GameCompat() {
	}

	public static @Nullable Screen screen(Minecraft minecraft) {
		return minecraft.screen;
	}

	public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
		minecraft.setScreen(screen);
	}

	/** Whether the HUD is turned off with F1. */
	public static boolean isHudHidden(Minecraft minecraft) {
		return minecraft.options.hideGui;
	}

	public static ChatComponent chat(Minecraft minecraft) {
		return minecraft.gui.getChat();
	}
}
