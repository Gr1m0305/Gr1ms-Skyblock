package io.github.gr1m0305.gsb.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.compat.GameCompat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

/** Draws the GUI elements of all features. */
public final class HudManager {
	private static final List<HudElement> ELEMENTS = new ArrayList<>();

	/** Where an element ends up on the screen, in scaled screen pixels. */
	public record Placement(float x, float y, float width, float height) {
	}

	private HudManager() {
	}

	public static void register(HudElement element) {
		ELEMENTS.add(element);
	}

	public static List<HudElement> elements() {
		return Collections.unmodifiableList(ELEMENTS);
	}

	public static void init() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(GrimsSkyblock.MOD_ID, "hud"),
				(graphics, deltaTracker) -> drawHud(graphics));

		// The HUD is blurred behind inventories and chests, so the elements are drawn again on top of those.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?>) {
				ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, tickDelta) -> drawElements(graphics));
			}
		});
	}

	private static void drawHud(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();

		if (GameCompat.isHudHidden(minecraft) || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Screen screen = GameCompat.screen(minecraft);

		if (screen instanceof AbstractContainerScreen<?> || screen instanceof PositionEditorScreen) {
			return;
		}

		drawElements(graphics);
	}

	private static void drawElements(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();

		for (HudElement element : ELEMENTS) {
			if (!element.isVisible()) {
				continue;
			}

			List<HudLine> lines = element.lines();

			if (lines.isEmpty()) {
				continue;
			}

			HudRenderer.Layout layout = HudRenderer.layout(minecraft.font, lines);
			Placement placement = place(element.position(), layout, graphics.guiWidth(), graphics.guiHeight());
			HudRenderer.draw(graphics, minecraft.font, lines, layout, placement.x(), placement.y(), element.position().scale);
		}
	}

	/** Works out where an element is drawn, keeping it inside the screen when its saved position is not. */
	public static Placement place(Position position, HudRenderer.Layout layout, int screenWidth, int screenHeight) {
		float width = layout.width() * position.scale;
		float height = layout.height() * position.scale;
		float x = Math.clamp(position.x, 0f, Math.max(0f, screenWidth - width));
		float y = Math.clamp(position.y, 0f, Math.max(0f, screenHeight - height));
		return new Placement(x, y, width, height);
	}
}
