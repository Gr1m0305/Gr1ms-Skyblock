package io.github.gr1m0305.gsb.test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.config.ConfigManager;
import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudManager;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.gui.PositionEditorScreen;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/**
 * Runs the mod in a real client. Hypixel is imitated by putting its tab list lines, chat messages and items
 * into a singleplayer world. The screenshots end up in the run directory.
 */
public class GsbClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(minecraft -> {
			minecraft.options.guiScale().set(2);
			minecraft.resizeGui();
			GrimsSkyblock.get().farmingTracker().reset();
		});

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			// Waiting for the chunks through the test API would tie this to one Fabric API version, it was renamed in 26.2.
			context.waitFor(minecraft -> minecraft.player != null && minecraft.level != null);
			context.waitTicks(60);
			check(!SkyblockState.onGarden(), "a normal world must not count as the Garden");
			check(!tracker().isVisible() && !fortune().isVisible(), "nothing is shown outside of the Garden");

			context.runOnClient(minecraft -> addTabLines(minecraft, "Area: Garden", "Stats:", " Farming Fortune: ☘1203", " Wheat Fortune: ☘521"));
			// The inventory is not watched for a few seconds after joining a world.
			context.waitTicks(120);
			check(SkyblockState.onGarden(), "the Area line of the tab list puts the player on the Garden");
			check(tracker().isVisible() && fortune().isVisible(), "both elements are shown on the Garden");
			check(lines(context, fortune()).equals(List.of("Farming Fortune: ☘1724")), "fortune line was " + lines(context, fortune()));

			// Crops that went into the sacks.
			sendChat(context, Component.literal("[Sacks] ")
					.append(Component.literal("+1,246 items").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(
							"Added items:\n  +1,234 Wheat (Agronomy Sack)\n  +12 Enchanted Melon Slice (Enchanted Agronomy Sack)\n\nThis message can be disabled in the settings.")))))
					.append(Component.literal(", "))
					.append(Component.literal("-5 items").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(
							"Removed items:\n  -5 Seeds (Agronomy Sack)")))))
					.append(Component.literal(". (Last 5s.)")));
			// A pest and selling to an NPC.
			sendChat(context, Component.literal("You received 7x Enchanted Potato for killing a Locust!"));
			sendChat(context, Component.literal("You sold Enchanted Melon Slice x64 for 20,480 Coins!"));
			// Items picked up into the inventory.
			world.getServer().runCommand("give @a minecraft:melon_slice[minecraft:custom_data={id:\"ENCHANTED_MELON\"}] 20");
			world.getServer().runCommand("give @a minecraft:paper[minecraft:custom_data={id:\"OVERCLOCKER_3000\"}] 1");
			context.waitTicks(10);
			// Items taken out of the sacks are not farmed.
			sendChat(context, Component.literal("Moved 3 Enchanted Hay Bale from your Sacks to your inventory."));
			world.getServer().runCommand("give @a minecraft:hay_block[minecraft:custom_data={id:\"ENCHANTED_HAY_BALE\"}] 3");
			context.waitTicks(10);

			List<String> trackerLines = lines(context, tracker());
			System.out.println("[GSB test] tracker lines: " + trackerLines);
			check(trackerLines.getFirst().equals("Farming Profit Tracker"), "title");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("1,234x|") && line.contains("Wheat")), "wheat from the sack message");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("32x|") && line.contains("Melon")), "12 melon from sacks plus 20 from the inventory");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("1x|") && line.contains("Overclocker")), "overclocker from the inventory");
			check(trackerLines.stream().noneMatch(line -> line.contains("Hay")), "items taken out of the sacks are not counted");
			check(trackerLines.stream().noneMatch(line -> line.contains("Seeds")), "items removed from the sacks are not counted");
			check(trackerLines.contains("Pests killed: 1"), "pest kill");
			check(trackerLines.contains("Sold to NPC: 20,480 coins"), "npc sale");
			context.takeScreenshot("1-hud");

			// The elements stay visible on top of inventories.
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("2-hud-over-inventory");
			context.setScreen(() -> null);

			// /gsb opens the config menu.
			context.runOnClient(minecraft -> minecraft.getConnection().sendCommand("gsb"));
			context.waitTicks(5);
			check(context.computeOnClient(minecraft -> GameCompat.screen(minecraft) instanceof MoulConfigScreenComponent), "/gsb opens the config menu");
			context.takeScreenshot("3-config");
			context.runOnClient(minecraft -> {
				showOption("fortuneDisplay");
				showOption("profitTracker");
			});
			context.waitTicks(20);
			context.takeScreenshot("4-config-farming");
			context.setScreen(() -> null);
			context.waitTicks(2);

			// /gsb gui opens the editor, where elements can be dragged, resized and reset.
			context.runOnClient(minecraft -> minecraft.getConnection().sendCommand("gsb gui"));
			context.waitTicks(5);
			check(context.computeOnClient(minecraft -> GameCompat.screen(minecraft) instanceof PositionEditorScreen), "/gsb gui opens the GUI editor");
			Position position = tracker().position();
			double guiScale = context.computeOnClient(minecraft -> (double) minecraft.getWindow().getGuiScale());
			context.getInput().setCursorPos((position.x + 20) * guiScale, (position.y + 15) * guiScale);
			context.waitTick();
			context.getInput().holdMouse(0);
			context.waitTick();
			context.getInput().moveCursor(200 * guiScale, 40 * guiScale);
			context.waitTick();
			context.getInput().releaseMouse(0);
			context.waitTick();
			check(position.x == 210 && position.y == 120, "dragging moved the tracker to " + position.x + ", " + position.y);
			context.getInput().scroll(1);
			context.waitTick();
			context.getInput().scroll(1);
			context.waitTick();
			check(Math.abs(position.scale - 1.2f) < 0.001f, "scrolling resized the tracker to " + position.scale);
			context.takeScreenshot("5-gui-editor");
			context.getInput().pressMouse(1);
			context.waitTick();
			check(position.x == 10 && position.y == 80 && position.scale == 1f, "right-click resets the position");
			context.setScreen(() -> null);

			// Resetting the tracker.
			context.runOnClient(minecraft -> minecraft.getConnection().sendCommand("gsb resetfarming"));
			context.waitTicks(10);
			List<String> afterReset = lines(context, tracker());
			check(afterReset.contains("Nothing farmed yet") && afterReset.contains("Pests killed: 0") && afterReset.contains("Sold to NPC: 0 coins"),
					"tracker after reset: " + afterReset);
			context.takeScreenshot("6-after-reset");
		}
	}

	private static HudElement tracker() {
		return HudManager.elements().get(0);
	}

	private static HudElement fortune() {
		return HudManager.elements().get(1);
	}

	/** The text of an element, the cells of table rows joined by {@code |}. */
	private static List<String> lines(ClientGameTestContext context, HudElement element) {
		return context.computeOnClient(minecraft -> element.lines().stream()
				.map(line -> String.join("|", line.cells().stream().map(Component::getString).toList()).trim())
				.toList());
	}

	private static void sendChat(ClientGameTestContext context, Component message) {
		context.runOnClient(minecraft -> minecraft.getConnection().handleSystemChat(new ClientboundSystemChatPacket(message, false)));
	}

	/** Hypixel fills the tab list with fake players whose display names are the lines of its widgets. */
	@SuppressWarnings("unchecked")
	private static void addTabLines(Minecraft minecraft, String... lines) {
		Set<PlayerInfo> players = (Set<PlayerInfo>) readField(ClientPacketListener.class, "listedPlayers", minecraft.getConnection());

		for (int i = 0; i < lines.length; i++) {
			PlayerInfo player = new PlayerInfo(new GameProfile(UUID.randomUUID(), "!line" + i), false);
			player.setTabListDisplayName(Component.literal(lines[i]));
			players.add(player);
		}
	}

	/** Selects the category of an accordion of the Farming category and opens it. */
	private static void showOption(String fieldName) {
		MoulConfigEditor<?> editor = (MoulConfigEditor<?>) readField(ConfigManager.class, "editor", GrimsSkyblock.get().config());

		try {
			check(editor.goToOption(editor.getOptionFromField(FarmingCategory.class.getField(fieldName))), "config option " + fieldName + " exists");
		} catch (NoSuchFieldException e) {
			throw new AssertionError(e);
		}
	}

	private static Object readField(Class<?> owner, String name, Object instance) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(instance);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
