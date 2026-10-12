package io.github.gr1m0305.gsb.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.authlib.GameProfile;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.config.ConfigManager;
import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.config.categories.GeneralCategory;
import io.github.gr1m0305.gsb.data.ItemInfo;
import io.github.gr1m0305.gsb.data.LegacyMaterials;
import io.github.gr1m0305.gsb.features.farming.FarmingStat;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.HudManager;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.gui.PositionEditorScreen;
import io.github.gr1m0305.gsb.skyblock.ItemIcons;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Runs the mod in a real client. Hypixel is imitated by putting its tab list lines, chat messages and items
 * into a singleplayer world. The screenshots end up in the run directory.
 */
public class GsbClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// The game still tells listeners about every sound when the volume is off, so the test can stay quiet.
		List<String> sounds = new CopyOnWriteArrayList<>();
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(minecraft -> {
			minecraft.options.guiScale().set(2);
			minecraft.resizeGui();
			minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
			minecraft.getSoundManager().addListener((sound, soundSet, range) -> sounds.add(sound.getIdentifier().toString()));
			// Start from the settings of a fresh install, whatever an earlier run left in the config file.
			GrimsSkyblock.get().config().get().farming = new FarmingCategory();
			GrimsSkyblock.get().config().get().general = new GeneralCategory();
			GrimsSkyblock.get().farmingTracker().reset();
		});

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			// Waiting for the chunks through the test API would tie this to one Fabric API version, it was renamed in 26.2.
			context.waitFor(minecraft -> minecraft.player != null && minecraft.level != null);
			context.waitTicks(60);
			check(!SkyblockState.onGarden(), "a normal world must not count as the Garden");
			check(!tracker().isVisible() && !stats().isVisible(), "nothing is shown outside of the Garden");
			checkIcons(context);
			GeneralCategory.PetDisplay petSettings = GrimsSkyblock.get().config().get().general.petDisplay;
			check(!petSettings.enabled && !pet().isVisible(), "the pet display is off by default");
			context.runOnClient(minecraft -> petSettings.enabled = true);
			check(!SkyblockState.onSkyblock() && !pet().isVisible(), "the pet display is not shown away from SkyBlock");

			PlayerInfo areaLine = context.computeOnClient(minecraft ->
					addTabLines(minecraft, "Area: Garden", "Stats:", " Speed: ✦400", " Farming Fortune: ☘1203", " Wheat Fortune: ☘521").getFirst());
			// Hypixel colours the name of a pet by its rarity, gold is legendary.
			List<PlayerInfo> petLines = context.computeOnClient(minecraft -> addTabLines(minecraft, Component.literal("Pet:"),
					Component.literal(" ").append(Component.literal("[Lvl 87] ").withStyle(ChatFormatting.GRAY)).append(Component.literal("Mooshroom Cow").withStyle(ChatFormatting.GOLD)),
					Component.literal(" Yellow Bandana"),
					Component.literal(" 153,251.5/250k XP (61.3%)")));
			// The inventory is not watched for a few seconds after joining a world.
			context.waitTicks(120);
			check(SkyblockState.onGarden(), "the Area line of the tab list puts the player on the Garden");

			// Every feature is off until it is turned on in the menu.
			check(!tracker().isEnabled() && !stats().isEnabled() && !alert().isEnabled(), "all features are off by default");
			check(!tracker().isVisible() && !stats().isVisible(), "features that are off show nothing on the Garden");
			context.runOnClient(minecraft -> {
				FarmingCategory farming = GrimsSkyblock.get().config().get().farming;
				farming.profitTracker.enabled = true;
				farming.statTracker.enabled = true;
				farming.pestCooldown.enabled = true;
			});
			context.waitTicks(10);
			check(tracker().isVisible() && stats().isVisible(), "both elements are shown on the Garden once they are turned on");
			check("Farming Profit Tracker".equals(tracker().title()) && "Farming Stats".equals(stats().title()), "both are panels with a title");
			check(alert().title() == null, "the alert is bare text");

			// The pet display has the pet of the tab list with a bar for the way to its next level.
			check(SkyblockState.onSkyblock() && pet().isVisible() && "Pet".equals(pet().title()), "the pet display is shown on SkyBlock");
			check(lines(context, pet()).equals(List.of("[Lvl 87] Mooshroom Cow")), "pet line was " + lines(context, pet()));
			HudLine petLine = context.computeOnClient(minecraft -> pet().lines().getFirst());
			check(petLine.bar() != null && Math.abs(petLine.bar().progress() - 0.613f) < 0.0001f && petLine.bar().text().getString().equals("61.3%"),
					"the bar of the pet was " + petLine.bar());
			check(petLine.cells().getFirst().getSiblings().stream().anyMatch(part -> part.getString().equals("Mooshroom Cow")
					&& part.getStyle().getColor() != null && part.getStyle().getColor().getValue() == 0xFFAA00), "the name of the pet has the colour of its rarity");
			// The picture of the pet is downloaded, which only works with a connection to the internet.
			long pictureWaitUntil = System.nanoTime() + 15_000_000_000L;

			while (System.nanoTime() < pictureWaitUntil && !context.computeOnClient(minecraft -> pet().lines().getFirst().icon().is(Items.PLAYER_HEAD))) {
				context.waitTicks(5);
			}

			ItemStack petIcon = context.computeOnClient(minecraft -> pet().lines().getFirst().icon());
			System.out.println("[GSB test] the pet is drawn as " + petIcon.getItem() + (petIcon.has(DataComponents.PROFILE) ? " with a texture" : ""));
			check(petIcon.is(Items.BONE) || petIcon.has(DataComponents.PROFILE), "the pet is a bone or a head with its texture");

			// The stat tracker shows the stats picked in the config, in that order.
			List<String> defaultStats = statLines(context);
			check(defaultStats.equals(List.of("Total Fortune: ☘1,724", "Speed: ✦400", "Yaw: -141.15", "Pitch: 3.30")), "default stats were " + defaultStats);
			check(context.computeOnClient(minecraft -> stats().lines().stream().allMatch(line -> line.icon() != null)), "every stat has an icon");
			List<FarmingStat> picked = GrimsSkyblock.get().config().get().farming.statTracker.stats;
			context.runOnClient(minecraft -> {
				picked.clear();
				picked.addAll(List.of(FarmingStat.PITCH, FarmingStat.CROP_FORTUNE, FarmingStat.FARMING_FORTUNE, FarmingStat.BONUS_PEST_CHANCE));
			});
			List<String> pickedStats = statLines(context);
			check(pickedStats.equals(List.of("Pitch: 3.30", "Wheat Fortune: ☘521", "Farming Fortune: ☘1,203", "Bonus Pest Chance: not in tab list")),
					"picked stats were " + pickedStats);
			context.runOnClient(minecraft -> picked.clear());
			check(lines(context, stats()).equals(List.of("No stats picked, add some in /gsb")), "without stats: " + lines(context, stats()));
			context.runOnClient(minecraft -> picked.addAll(List.of(FarmingStat.values())));
			context.waitTicks(20);
			context.takeScreenshot("0-all-stats");
			context.runOnClient(minecraft -> {
				picked.clear();
				picked.addAll(FarmingStat.DEFAULTS);
			});

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

			// The tracker only rebuilds its text every three seconds, so wait for it to pick all of that up.
			List<HudLine> shown = waitForTrackerRefresh(context);
			List<String> trackerLines = lines(context, tracker());
			System.out.println("[GSB test] tracker lines: " + trackerLines);
			check(context.computeOnClient(minecraft -> tracker().lines().stream().allMatch(line -> line.isRow() == (line.icon() != null))),
					"the items of the tracker have icons, its totals do not");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("1,234x|") && line.contains("Wheat")), "wheat from the sack message");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("32x|") && line.contains("Melon")), "12 melon from sacks plus 20 from the inventory");
			check(trackerLines.stream().anyMatch(line -> line.startsWith("1x|") && line.contains("Overclocker")), "overclocker from the inventory");
			check(trackerLines.stream().noneMatch(line -> line.contains("Hay")), "items taken out of the sacks are not counted");
			check(trackerLines.stream().noneMatch(line -> line.contains("Seeds")), "items removed from the sacks are not counted");
			check(trackerLines.contains("Pests killed: 1"), "pest kill");
			check(trackerLines.contains("Sold to NPC: 20,480 coins"), "npc sale");

			// Crops in the sacks that get compacted turn into what they became, they are not counted on top of it.
			sendChat(context, Component.literal("[Sacks] ")
					.append(hovering("+1 item", "Added items:\n  +1 Enchanted Wheat (Enchanted Agronomy Sack)"))
					.append(Component.literal(", "))
					.append(hovering("-160 items", "Removed items:\n  -160 Wheat (Agronomy Sack)"))
					.append(Component.literal(". (Last 5s.)")));
			waitForTrackerRefresh(context);
			List<String> compacted = lines(context, tracker());
			System.out.println("[GSB test] tracker lines after compacting: " + compacted);
			check(compacted.stream().anyMatch(line -> line.startsWith("1,074x|") && line.contains("Wheat")), "160 of the 1,234 wheat were used up");
			check(compacted.stream().anyMatch(line -> line.startsWith("1x|") && line.contains("Wheat")), "one enchanted wheat was made of them");

			// What leaves the sacks while a menu of the server is open was sold or moved, it is not gone.
			context.setScreen(() -> {
				Inventory inventory = Minecraft.getInstance().player.getInventory();
				return new ContainerScreen(ChestMenu.threeRows(0, inventory), inventory, Component.literal("Bazaar"));
			});
			context.waitTicks(5);
			sendChat(context, Component.literal("[Sacks] ")
					.append(hovering("-1,000 items", "Removed items:\n  -1,000 Wheat (Agronomy Sack)"))
					.append(Component.literal(". (Last 5s.)")));
			context.setScreen(() -> null);
			waitForTrackerRefresh(context);
			check(lines(context, tracker()).stream().anyMatch(line -> line.startsWith("1,074x|")), "selling from the sacks took wheat out of the tracker: "
					+ lines(context, tracker()));

			// A few seconds after the last sign of farming the tracker pauses, which it shows right away.
			context.waitFor(minecraft -> text(tracker().lines()).getLast().endsWith("(Paused)"), 200);
			System.out.println("[GSB test] the tracker paused: " + lines(context, tracker()).getLast());
			shown = context.computeOnClient(minecraft -> tracker().lines());

			// However often it is drawn, the text changes no more than once every three seconds.
			List<Long> refreshTimes = new ArrayList<>();
			Set<String> uptimesWhilePaused = new HashSet<>();
			long watchUntil = System.nanoTime() + 7_500_000_000L;

			while (System.nanoTime() < watchUntil) {
				context.waitTick();
				List<HudLine> now = context.computeOnClient(minecraft -> tracker().lines());

				if (now != shown) {
					shown = now;
					refreshTimes.add(System.nanoTime() / 1_000_000);
					uptimesWhilePaused.add(text(now).getLast());
				}
			}

			System.out.println("[GSB test] tracker refreshed at " + refreshTimes);
			check(refreshTimes.size() >= 2, "the tracker keeps refreshing, saw " + refreshTimes.size() + " refreshes in 7.5 seconds");

			for (int i = 1; i < refreshTimes.size(); i++) {
				long gap = refreshTimes.get(i) - refreshTimes.get(i - 1);
				// A refresh is noticed up to one tick late, hence the small allowance.
				check(gap >= 2900, "refreshes were only " + gap + " ms apart");
			}

			context.takeScreenshot("1-hud");

			// The uptime stood still all that time, and holding the mouse button to farm on ends the pause at once.
			check(uptimesWhilePaused.size() == 1 && uptimesWhilePaused.iterator().next().endsWith("(Paused)"), "the uptime while paused was " + uptimesWhilePaused);
			context.getInput().holdMouse(0);
			context.waitTicks(3);
			check(!lines(context, tracker()).getLast().contains("Paused"), "farming on did not end the pause: " + lines(context, tracker()).getLast());
			context.getInput().releaseMouse(0);

			// The pest cooldown runs out. With only seconds left, the mod counts them down itself.
			check(lines(context, alert()).isEmpty() && !alert().isVisible(), "no alert while nothing happened");
			PlayerInfo cooldownLine = context.computeOnClient(minecraft -> addTabLines(minecraft, "Pests:", " Cooldown: 3s").getLast());
			long cooldownAdded = System.nanoTime();
			context.waitFor(minecraft -> alert().isVisible(), 400);
			long alertStarted = System.nanoTime();
			long alertAfter = (alertStarted - cooldownAdded) / 1_000_000;
			System.out.println("[GSB test] alert came " + alertAfter + " ms after a 3s cooldown showed up");
			check(alertAfter >= 2800 && alertAfter <= 4500, "alert came " + alertAfter + " ms after a 3s cooldown showed up");
			check(lines(context, alert()).equals(List.of("Pest Cooldown")), "alert text was " + lines(context, alert()));
			context.takeScreenshot("7-pest-cooldown-alert");
			context.waitFor(minecraft -> !alert().isVisible(), 400);
			long alertShownFor = (System.nanoTime() - alertStarted) / 1_000_000;
			System.out.println("[GSB test] alert was shown for " + alertShownFor + " ms with " + dings(sounds) + " dings");
			check(alertShownFor >= 1800 && alertShownFor <= 3500, "the alert was shown for " + alertShownFor + " ms");
			check(dings(sounds) == 10, "the alert played " + dings(sounds) + " dings");

			// A new cooldown starts, and this time the tab list is what says it is over.
			sounds.clear();
			context.runOnClient(minecraft -> cooldownLine.setTabListDisplayName(Component.literal(" Cooldown: 2m 10s")));
			context.waitTicks(30);
			check(!alert().isVisible() && dings(sounds) == 0, "a running cooldown is no reason for an alert");
			context.runOnClient(minecraft -> cooldownLine.setTabListDisplayName(Component.literal(" Cooldown: READY")));
			context.waitFor(minecraft -> alert().isVisible(), 100);
			context.waitFor(minecraft -> !alert().isVisible(), 400);
			check(dings(sounds) == 10, "the alert for READY played " + dings(sounds) + " dings");
			// READY staying on the tab list is not a new alert.
			context.waitTicks(40);
			check(!alert().isVisible() && dings(sounds) == 10, "the alert only plays once");

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
				showOption("statTracker");
				showOption("profitTracker");
			});
			context.waitTicks(20);
			context.takeScreenshot("4-config-farming");

			// The list the stats of the stat tracker are picked from.
			context.runOnClient(minecraft -> showOption(FarmingCategory.StatTracker.class, "stats"));
			context.waitTicks(20);
			context.takeScreenshot("4-config-stat-tracker");

			// The pet display is not a farming feature and has a category of its own.
			context.runOnClient(minecraft -> showOption(GeneralCategory.class, "petDisplay"));
			context.waitTicks(20);
			context.takeScreenshot("4-config-general");

			// The GitHub button sits in the top right corner of the menu. Only hover it, clicking would open a browser.
			check(GrimsSkyblock.get().config().get().getSocials().size() == 1, "the menu has the GitHub button");
			int[] button = context.computeOnClient(minecraft -> {
				int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
				int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
				int scale = minecraft.getWindow().getGuiScale();
				// The same layout the menu uses for itself.
				int menuWidth = Math.min(scaledWidth - 100 / scale, 500);
				int menuHeight = Math.min(scaledHeight - 100 / scale, 400);
				int buttonX = (scaledWidth - menuWidth) / 2 + menuWidth - 23 + 8;
				int buttonY = (scaledHeight - menuHeight) / 2 + 7 + 8;
				return new int[] {buttonX * scale, buttonY * scale};
			});
			context.getInput().setCursorPos(button[0], button[1]);
			context.waitTicks(5);
			context.takeScreenshot("4-config-github-button");

			// The test button of the alert closes the menu and plays it.
			context.runOnClient(minecraft -> showOption("pestCooldown"));
			context.waitTicks(20);
			context.takeScreenshot("4-config-pest-cooldown");
			sounds.clear();
			context.runOnClient(minecraft -> GrimsSkyblock.get().config().get().farming.pestCooldown.test.run());
			context.waitTicks(3);
			check(context.computeOnClient(minecraft -> GameCompat.screen(minecraft) == null), "the test button closes the menu");
			check(alert().isVisible(), "the test button shows the alert");
			context.waitFor(minecraft -> !alert().isVisible(), 400);
			check(dings(sounds) == 10, "the test alert played " + dings(sounds) + " dings");

			// The slider changes how long the alert lasts.
			sounds.clear();
			context.runOnClient(minecraft -> {
				GrimsSkyblock.get().config().get().farming.pestCooldown.alertSeconds = 0.5f;
				GrimsSkyblock.get().pestCooldownAlert().test();
			});
			context.waitFor(minecraft -> !alert().isVisible(), 400);
			check(dings(sounds) == 3, "half a second of alert played " + dings(sounds) + " dings");
			context.runOnClient(minecraft -> GrimsSkyblock.get().config().get().farming.pestCooldown.alertSeconds = 2f);
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
			check(position.x == 210 && position.y == 140, "dragging moved the tracker to " + position.x + ", " + position.y);
			context.getInput().scroll(1);
			context.waitTick();
			context.getInput().scroll(1);
			context.waitTick();
			check(Math.abs(position.scale - 1.2f) < 0.001f, "scrolling resized the tracker to " + position.scale);
			context.takeScreenshot("5-gui-editor");
			context.getInput().pressMouse(1);
			context.waitTick();
			check(position.x == 10 && position.y == 100 && position.scale == 1f, "right-click resets the position");

			// The alert text starts out centred and can be dragged away like everything else.
			Position alertPosition = alert().position();
			check(alertPosition.centered && alertPosition.y == 70 && alertPosition.scale == 3f, "the alert starts big and centred");
			double middle = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScaledWidth() / 2.0);
			context.getInput().setCursorPos(middle * guiScale, 80 * guiScale);
			context.waitTick();
			context.getInput().holdMouse(0);
			context.waitTick();
			context.getInput().moveCursor(-100 * guiScale, 60 * guiScale);
			context.waitTick();
			context.getInput().releaseMouse(0);
			context.waitTick();
			check(!alertPosition.centered && alertPosition.y == 130 && alertPosition.x < middle - 100,
					"dragging moved the alert to " + alertPosition.x + ", " + alertPosition.y + " centred: " + alertPosition.centered);
			context.getInput().pressMouse(1);
			context.waitTick();
			check(alertPosition.centered && alertPosition.y == 70 && alertPosition.scale == 3f, "right-click centres the alert again");
			context.setScreen(() -> null);

			// Resetting the tracker.
			context.runOnClient(minecraft -> minecraft.getConnection().sendCommand("gsb resetfarming"));
			context.waitTicks(10);
			List<String> afterReset = lines(context, tracker());
			check(afterReset.contains("Nothing farmed yet") && afterReset.contains("Pests killed: 0") && afterReset.contains("Sold to NPC: 0 coins"),
					"tracker after reset: " + afterReset);
			context.takeScreenshot("6-after-reset");

			// Away from the Garden the farming elements go, the pet display stays.
			context.runOnClient(minecraft -> areaLine.setTabListDisplayName(Component.literal("Area: Hub")));
			context.waitTicks(10);
			check(SkyblockState.onSkyblock() && !SkyblockState.onGarden(), "the Hub is SkyBlock, but not the Garden");
			check(!tracker().isVisible() && !stats().isVisible() && pet().isVisible(), "only the pet display is shown in the Hub");
			context.takeScreenshot("8-pet-in-the-hub");

			// Where the tab list has no Pet widget the pet that was out stays on the screen.
			context.runOnClient(minecraft -> petLines.getFirst().setTabListDisplayName(null));
			context.waitTicks(10);
			check(lines(context, pet()).equals(List.of("[Lvl 87] Mooshroom Cow")), "without the widget the pet line was " + lines(context, pet()));
			context.runOnClient(minecraft -> {
				petLines.getFirst().setTabListDisplayName(Component.literal("Pet:"));
				petLines.get(1).setTabListDisplayName(Component.literal(" [Lvl 100] ").append(Component.literal("Elephant").withStyle(ChatFormatting.DARK_PURPLE)));
				petLines.get(3).setTabListDisplayName(Component.literal(" MAX LEVEL"));
			});
			context.waitTicks(10);
			HudLine maxedPet = context.computeOnClient(minecraft -> pet().lines().getFirst());
			check(text(List.of(maxedPet)).equals(List.of("[Lvl 100] Elephant")) && maxedPet.bar() != null && maxedPet.bar().progress() == 1f
					&& maxedPet.bar().text().getString().equals("MAX LEVEL"), "a pet at its highest level was " + text(List.of(maxedPet)) + " with " + maxedPet.bar());
			context.takeScreenshot("9-pet-max-level");
			context.runOnClient(minecraft -> {
				petLines.get(1).setTabListDisplayName(Component.literal(" No pet selected"));
				petLines.get(2).setTabListDisplayName(null);
				petLines.get(3).setTabListDisplayName(null);
			});
			context.waitTicks(10);
			check(lines(context, pet()).equals(List.of("No pet selected")), "without a pet the line was " + lines(context, pet()));
		}
	}

	private static HudElement tracker() {
		return HudManager.elements().get(0);
	}

	private static HudElement stats() {
		return HudManager.elements().get(1);
	}

	private static HudElement alert() {
		return HudManager.elements().get(2);
	}

	private static HudElement pet() {
		return HudManager.elements().get(3);
	}

	/** How many of the sounds that were played are the ding of the alert. */
	private static long dings(List<String> sounds) {
		return sounds.stream().filter("minecraft:entity.arrow.hit_player"::equals).count();
	}

	/** The text of an element, the cells of table rows joined by {@code |}. */
	private static List<String> lines(ClientGameTestContext context, HudElement element) {
		return context.computeOnClient(minecraft -> text(element.lines()));
	}

	private static List<String> text(List<HudLine> lines) {
		return lines.stream()
				.map(line -> String.join("|", line.cells().stream().map(Component::getString).toList()).trim())
				.toList();
	}

	/** The text of the stat tracker with the player looking in a known direction. The game counts a yaw of 218.85 as -141.15. */
	private static List<String> statLines(ClientGameTestContext context) {
		return context.computeOnClient(minecraft -> {
			minecraft.player.setYRot(218.85f);
			minecraft.player.setXRot(3.3f);
			return text(stats().lines());
		});
	}

	/** The icons of SkyBlock items are made from what the Hypixel API says about them. */
	private static void checkIcons(ClientGameTestContext context) {
		context.runOnClient(minecraft -> {
			for (String name : LegacyMaterials.modernNames()) {
				check(BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(name)), "this version of the game has no item called " + name);
			}

			check(ItemIcons.of(item("INK_SACK", 3, false, "", "")).is(Items.COCOA_BEANS), "cocoa beans are the third ink sack");
			check(ItemIcons.of(item("HAY_BLOCK", 0, false, "", "")).is(Items.HAY_BLOCK), "names that never changed");
			ItemStack melon = ItemIcons.of(item("MELON", 0, true, "", ""));
			check(melon.is(Items.MELON_SLICE) && melon.hasFoil(), "an enchanted melon slice shines");
			check(!ItemIcons.of(item("MELON", 0, false, "", "")).hasFoil(), "a melon slice does not shine");
			ItemStack head = ItemIcons.of(item("SKULL_ITEM", 3, false, "ewogICJ0aW1lc3RhbXAiIDogMTY4", ""));
			check(head.is(Items.PLAYER_HEAD) && head.has(DataComponents.PROFILE), "heads get their texture");

			// A model is only used while a resource pack has it, Hypixel's are not there in this test.
			ItemStack modelled = ItemIcons.of(item("PAPER", 0, false, "", "minecraft:diamond"));
			check(Identifier.withDefaultNamespace("diamond").equals(modelled.get(DataComponents.ITEM_MODEL)), "a model the game has is used");
			ItemStack plain = ItemIcons.of(item("PAPER", 0, false, "", "hypixel_skyblock:item/uncategorized/mutant_nether_wart"));
			check(plain.is(Items.PAPER) && Identifier.withDefaultNamespace("paper").equals(plain.get(DataComponents.ITEM_MODEL)),
					"a model the game does not have is left out");

			check(ItemIcons.of(item("NOT_A_MATERIAL", 0, false, "", "")).is(Items.PAPER), "unknown materials are paper");
			check(ItemIcons.of((ItemInfo) null).is(Items.PAPER), "unknown items are paper");
		});
	}

	private static ItemInfo item(String material, int durability, boolean glowing, String skin, String model) {
		return new ItemInfo("TEST", "Test", "COMMON", 0, new ItemInfo.Appearance(material, durability, glowing, skin, model));
	}

	/** Waits until the tracker has rebuilt its text and returns the new lines. */
	private static List<HudLine> waitForTrackerRefresh(ClientGameTestContext context) {
		List<HudLine> before = context.computeOnClient(minecraft -> tracker().lines());
		context.waitFor(minecraft -> tracker().lines() != before, 1200);
		return context.computeOnClient(minecraft -> tracker().lines());
	}

	/** A part of a chat message that shows more text when the mouse is over it, which is where [Sacks] messages list their items. */
	private static Component hovering(String text, String hoverText) {
		return Component.literal(text).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
	}

	private static void sendChat(ClientGameTestContext context, Component message) {
		context.runOnClient(minecraft -> minecraft.getConnection().handleSystemChat(new ClientboundSystemChatPacket(message, false)));
	}

	private static List<PlayerInfo> addTabLines(Minecraft minecraft, String... lines) {
		return addTabLines(minecraft, Arrays.stream(lines).map(Component::literal).toArray(Component[]::new));
	}

	/**
	 * Hypixel fills the tab list with fake players whose display names are the lines of its widgets. The game
	 * sorts them by their names, which is what puts the lines in order.
	 */
	@SuppressWarnings("unchecked")
	private static List<PlayerInfo> addTabLines(Minecraft minecraft, Component... lines) {
		Set<PlayerInfo> players = (Set<PlayerInfo>) readField(ClientPacketListener.class, "listedPlayers", minecraft.getConnection());
		List<PlayerInfo> added = new ArrayList<>();

		for (Component line : lines) {
			PlayerInfo player = new PlayerInfo(new GameProfile(UUID.randomUUID(), String.format("!line%02d", players.size())), false);
			player.setTabListDisplayName(line);
			players.add(player);
			added.add(player);
		}

		return added;
	}

	/** Selects the category of an accordion of the Farming category and opens it. */
	private static void showOption(String fieldName) {
		showOption(FarmingCategory.class, fieldName);
	}

	/** Scrolls the config menu to an option, opening what it is inside of. */
	private static void showOption(Class<?> owner, String fieldName) {
		MoulConfigEditor<?> editor = (MoulConfigEditor<?>) readField(ConfigManager.class, "editor", GrimsSkyblock.get().config());

		try {
			check(editor.goToOption(editor.getOptionFromField(owner.getField(fieldName))), "config option " + fieldName + " exists");
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
