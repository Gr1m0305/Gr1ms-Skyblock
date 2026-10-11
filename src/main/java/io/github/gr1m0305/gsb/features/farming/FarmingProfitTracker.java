package io.github.gr1m0305.gsb.features.farming;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.data.BazaarPrices;
import io.github.gr1m0305.gsb.data.ItemInfo;
import io.github.gr1m0305.gsb.data.ItemRepository;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import io.github.gr1m0305.gsb.util.ChatUtil;
import io.github.gr1m0305.gsb.util.NumberFormats;
import io.github.gr1m0305.gsb.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * Tracks what farming on the Garden earns: the crops farmed, the drops of vacuumed pests and the coins
 * from selling items to NPCs.
 */
public final class FarmingProfitTracker {
	private static final long SAVE_INTERVAL_MILLIS = 60_000;
	private static final long BAZAAR_REFRESH_MILLIS = 10 * 60_000;
	private static final long LINE_REFRESH_MILLIS = 250;
	/** Farming this long without a single item being counted means the sack messages are probably turned off. */
	private static final long SACK_HINT_AFTER_MILLIS = 120_000;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Supplier<FarmingCategory.ProfitTracker> config;
	private final ItemRepository items;
	private final BazaarPrices bazaar;
	private final Path file;
	private final FarmingTrackerLogic logic;
	private final InventoryWatcher inventory = new InventoryWatcher();
	private final Hud hud = new Hud();
	private List<HudLine> lines = List.of();
	private long linesBuiltAt;
	private long lastTick = System.currentTimeMillis();
	private long lastSave = System.currentTimeMillis();
	private long farmingWithoutGainMillis;
	private boolean sackMessageSeen;
	private boolean sackHintSent;

	public FarmingProfitTracker(Supplier<FarmingCategory.ProfitTracker> config, ItemRepository items, BazaarPrices bazaar, Path file) {
		this.config = config;
		this.items = items;
		this.bazaar = bazaar;
		this.file = file;
		this.logic = new FarmingTrackerLogic(load());
	}

	public HudElement hud() {
		return hud;
	}

	private boolean isTracking() {
		return config.get().enabled && SkyblockState.onGarden();
	}

	public void tick(Minecraft minecraft) {
		long now = System.currentTimeMillis();
		// Capped so a lag spike or a paused game does not add minutes of uptime.
		long elapsed = Math.clamp(now - lastTick, 0, 1000);
		lastTick = now;
		boolean tracking = isTracking() && minecraft.player != null;
		inventory.tick(minecraft, tracking, logic, now);

		if (tracking) {
			boolean farmingInput = GameCompat.screen(minecraft) == null && (minecraft.options.keyAttack.isDown() || minecraft.options.keyUse.isDown());
			logic.tick(now, elapsed, farmingInput);
			items.retryIfMissing();
			bazaar.refreshIfOlderThan(BAZAAR_REFRESH_MILLIS);
			checkSackMessages(now, elapsed, farmingInput);
		}

		if (now - lastSave >= SAVE_INTERVAL_MILLIS) {
			lastSave = now;

			if (logic.takeDirty()) {
				save();
			}
		}
	}

	private void checkSackMessages(long now, long elapsed, boolean farmingInput) {
		if (sackMessageSeen || sackHintSent || !farmingInput) {
			return;
		}

		farmingWithoutGainMillis = logic.millisSinceLastGain(now) < 5000 ? 0 : farmingWithoutGainMillis + elapsed;

		if (farmingWithoutGainMillis >= SACK_HINT_AFTER_MILLIS) {
			sackHintSent = true;
			ChatUtil.send("§eThe Farming Profit Tracker has not seen any items yet. If you are farming into your sacks, turn on "
					+ "§aSack Notifications §ein the SkyBlock settings (Personal → Chat Feedback).");
		}
	}

	/** Called for every chat message the server sends. */
	public void onChat(Component message) {
		String text = StringUtil.stripFormatting(message.getString());
		boolean sackMessage = FarmingChatParser.isSackMessage(text);

		if (sackMessage) {
			sackMessageSeen = true;
		}

		if (!isTracking()) {
			return;
		}

		long now = System.currentTimeMillis();
		FarmingChatParser.parseNpcSale(text).ifPresent(sale -> logic.soldToNpc(sale.coins(), sale.count()));
		FarmingChatParser.parsePestLoot(text).ifPresent(loot -> logic.pestKilled(now));

		// The sack messages only have item names. Without the item list those cannot be matched to the items in
		// the inventory, and guessing would leave wrong entries in the tracker for good.
		if (!items.isLoaded()) {
			return;
		}

		if (sackMessage) {
			Set<String> hoverTexts = new LinkedHashSet<>();
			collectHoverTexts(message, hoverTexts);

			for (String hoverText : hoverTexts) {
				for (FarmingChatParser.SackChange change : FarmingChatParser.parseSackHover(hoverText)) {
					logic.sackChanged(items.idFromName(change.item()), change.delta(), now);
				}
			}

			return;
		}

		FarmingChatParser.parseSackWithdrawal(text)
				.ifPresent(withdrawal -> logic.withdrawnFromSack(items.idFromName(withdrawal.item()), withdrawal.amount(), now));
	}

	/** The item list of a [Sacks] message is in the text shown when hovering over it. */
	private static void collectHoverTexts(Component component, Set<String> texts) {
		if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText(Component value)) {
			texts.add(StringUtil.stripFormatting(value.getString()));
		}

		for (Component sibling : component.getSiblings()) {
			collectHoverTexts(sibling, texts);
		}
	}

	public void reset() {
		FarmingTrackerData data = logic.data();

		// One misclick should not cost days of data, the numbers from before the reset stay next to the file.
		if (!data.items.isEmpty() || data.pestsKilled > 0 || data.npcCoins > 0 || data.uptimeMillis > 0) {
			save();

			try {
				Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				GrimsSkyblock.LOGGER.warn("Could not back up {}", file, e);
			}
		}

		logic.reset();
		linesBuiltAt = 0;
		save();
		ChatUtil.send("§aReset the Farming Profit Tracker.");
	}

	private FarmingTrackerData load() {
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				FarmingTrackerData data = GSON.fromJson(reader, FarmingTrackerData.class);

				if (data != null) {
					if (data.items == null) {
						data.items = new HashMap<>();
					}

					data.items.values().removeIf(count -> count == null);
					return data;
				}
			} catch (IOException | JsonParseException e) {
				GrimsSkyblock.LOGGER.error("Could not read {}, the Farming Profit Tracker starts empty", file, e);
			}
		}

		return new FarmingTrackerData();
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temporary, GSON.toJson(logic.data()));
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			GrimsSkyblock.LOGGER.error("Could not save {}", file, e);
		}
	}

	private double priceOf(String id) {
		return config.get().priceSource.price(items.get(id), bazaar.get(id));
	}

	private List<HudLine> buildLines() {
		FarmingTrackerLogic.Summary summary = logic.summarize(this::priceOf, config.get().itemsShown);
		List<HudLine> result = new ArrayList<>();
		result.add(HudLine.of("§e§lFarming Profit Tracker"));

		if (!items.isLoaded()) {
			result.add(HudLine.of(" §cWaiting for item data from the Hypixel API"));
		}

		for (FarmingTrackerLogic.ItemRow row : summary.rows()) {
			ItemInfo info = items.get(row.id());
			String name = items.displayName(row.id()).replace("Enchanted ", "Ench ");
			result.add(itemRow(row.count(), info != null ? info.rarityColor() : 'f', name, row.value()));
		}

		if (summary.rows().isEmpty()) {
			result.add(HudLine.of(" §7Nothing farmed yet"));
		}

		if (summary.hiddenItems() > 0) {
			result.add(HudLine.of(" §7+ " + summary.hiddenItems() + " more: §6" + NumberFormats.compact(summary.hiddenValue())));
		}

		addTotals(result, summary.pestsKilled(), summary.npcCoins(), summary.totalProfit(), summary.profitPerHour(), summary.uptimeMillis());
		return result;
	}

	private static HudLine itemRow(long count, char rarityColor, String name, double value) {
		return HudLine.row("§7" + NumberFormats.grouped(count) + "x", "§" + rarityColor + name, "§6" + NumberFormats.compact(value));
	}

	private static void addTotals(List<HudLine> result, long pestsKilled, double npcCoins, double totalProfit, double profitPerHour, long uptimeMillis) {
		result.add(HudLine.of("§7Pests killed: §e" + NumberFormats.grouped(pestsKilled)));
		result.add(HudLine.of("§7Sold to NPC: §6" + NumberFormats.grouped(npcCoins) + " coins"));
		result.add(HudLine.of("§eTotal Profit: §6" + NumberFormats.grouped(totalProfit) + " coins"));
		result.add(HudLine.of("§eProfit Per Hour: §6" + NumberFormats.grouped(profitPerHour) + " coins"));
		result.add(HudLine.of("§eTotal Uptime: §b" + NumberFormats.duration(uptimeMillis)));
	}

	/** Made-up numbers, so the GUI editor shows the tracker at a realistic size before anything was farmed. */
	private static List<HudLine> exampleLines() {
		List<HudLine> result = new ArrayList<>();
		result.add(HudLine.of("§e§lFarming Profit Tracker"));
		result.add(itemRow(76_549, 'a', "Ench Cocoa Beans", 36_743_520));
		result.add(itemRow(105_061, 'a', "Ench Melon Slice", 33_619_520));
		result.add(itemRow(23_056, 'a', "Ench Pumpkin", 36_889_600));
		result.add(itemRow(59_783, 'a', "Ench Carrot", 28_695_840));
		result.add(itemRow(61_769, 'a', "Ench Potato", 29_649_120));
		result.add(itemRow(84, '6', "Overclocker 3000", 21_000_000));
		addTotals(result, 4_383, 52_480_000, 186_597_600, 14_083_123, 47_700_000);
		return result;
	}

	private final class Hud extends HudElement {
		Hud() {
			super("Farming Profit Tracker", 10, 80);
		}

		@Override
		public Position position() {
			return config.get().position;
		}

		@Override
		public boolean isEnabled() {
			return config.get().enabled;
		}

		@Override
		public boolean isVisible() {
			return isTracking();
		}

		@Override
		public List<HudLine> lines() {
			long now = System.currentTimeMillis();

			// Sorting and formatting every frame would be wasteful, a few times per second looks just as live.
			if (now - linesBuiltAt >= LINE_REFRESH_MILLIS) {
				linesBuiltAt = now;
				lines = buildLines();
			}

			return lines;
		}

		@Override
		public List<HudLine> previewLines() {
			return logic.data().items.isEmpty() ? exampleLines() : lines();
		}
	}
}
