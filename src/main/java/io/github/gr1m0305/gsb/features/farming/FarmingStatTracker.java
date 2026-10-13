package io.github.gr1m0305.gsb.features.farming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.ItemIcons;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import io.github.gr1m0305.gsb.util.NumberFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Shows the farming stats picked in the config menu. Most of them are read from the tab list. */
public final class FarmingStatTracker extends HudElement {
	private static final List<HudLine> NO_STATS = List.of(HudLine.of("§7No stats picked, add some in /gsb"));

	private final Supplier<FarmingCategory.StatTracker> config;
	private final Map<Item, ItemStack> icons = new HashMap<>();
	private @Nullable List<String> readTabLines;
	private FarmingStatReader reader = new FarmingStatReader(List.of());
	private List<FarmingStat> shownStats = List.of();
	private int shownYaw;
	private int shownPitch;
	private List<HudLine> lines = List.of();

	public FarmingStatTracker(Supplier<FarmingCategory.StatTracker> config) {
		super("Farming Stat Tracker", FarmingCategory.StatTracker::defaultPosition);
		this.config = config;
	}

	@Override
	public String title() {
		return "Farming Stats";
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
		return isEnabled() && SkyblockState.onGarden();
	}

	@Override
	public List<HudLine> lines() {
		List<FarmingStat> stats = config.get().stats;
		int yaw = hundredths(true);
		int pitch = hundredths(false);
		boolean tabListRead = readTabList();

		// Building the lines on every frame would be wasteful, they only change with one of these.
		if (tabListRead || yaw != shownYaw || pitch != shownPitch || !stats.equals(shownStats)) {
			shownStats = List.copyOf(stats);
			shownYaw = yaw;
			shownPitch = pitch;
			lines = buildLines(stats, yaw, pitch, false);
		}

		return lines;
	}

	/** The editor is also open away from the Garden, where the tab list has none of the stats. */
	@Override
	public List<HudLine> previewLines() {
		readTabList();
		return buildLines(config.get().stats, hundredths(true), hundredths(false), true);
	}

	/** The tab list is read into a new list a few times per second, only look at it when that happened. */
	private boolean readTabList() {
		List<String> tabLines = SkyblockState.tabLines();

		if (tabLines == readTabLines) {
			return false;
		}

		readTabLines = tabLines;
		reader = new FarmingStatReader(tabLines);
		return true;
	}

	/** Where the player looks in hundredths of a degree, which is as exact as the tracker shows it. */
	private static int hundredths(boolean yaw) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null) {
			return 0;
		}

		// The game counts the yaw up and down without end while the player turns.
		return Math.round((yaw ? Mth.wrapDegrees(player.getYRot()) : player.getXRot()) * 100f);
	}

	/**
	 * @param examples whether stats the tab list does not have are shown with a made-up value
	 */
	private List<HudLine> buildLines(List<FarmingStat> stats, int yaw, int pitch, boolean examples) {
		if (stats.isEmpty()) {
			return NO_STATS;
		}

		List<HudLine> result = new ArrayList<>();

		for (FarmingStat stat : stats) {
			String value = switch (stat) {
				case YAW -> "§e" + NumberFormats.degrees(yaw / 100f);
				case PITCH -> "§e" + NumberFormats.degrees(pitch / 100f);
				default -> tabListValue(stat, examples);
			};

			result.add(HudLine.of("§f" + reader.label(stat) + ": " + value).withIcon(icon(stat)));
		}

		return result;
	}

	private String tabListValue(FarmingStat stat, boolean examples) {
		Optional<String> value = reader.value(stat);

		if (value.isEmpty() && examples) {
			value = Optional.of(example(stat));
		}

		// The Pests widget says READY once the cooldown is over.
		return value.map(text -> (stat == FarmingStat.PEST_COOLDOWN && text.startsWith("READY") ? "§a" : "§e") + text)
				.orElse("§cnot in tab list");
	}

	private static String example(FarmingStat stat) {
		return switch (stat) {
			case TOTAL_FORTUNE -> "☘1,724";
			case FARMING_FORTUNE -> "☘1,203";
			case CROP_FORTUNE -> "☘521";
			case SPEED -> "✦400";
			case BONUS_PEST_CHANCE -> "ൠ190%";
			// Without its symbol, which is a character only the resource pack of Hypixel has.
			case OVERBLOOM -> "25";
			case FARMING_WISDOM -> "☯48";
			case PEST_COOLDOWN -> "1m 58s";
			case YAW, PITCH -> throw new IllegalArgumentException(stat + " is not read from the tab list");
		};
	}

	private ItemStack icon(FarmingStat stat) {
		Item item = switch (stat) {
			case TOTAL_FORTUNE -> Items.GOLDEN_HOE;
			case FARMING_FORTUNE -> Items.WHEAT;
			case CROP_FORTUNE -> cropItem(reader.crop());
			case SPEED -> Items.SUGAR;
			case BONUS_PEST_CHANCE -> Items.SPIDER_EYE;
			// The game draws the stat with it too.
			case OVERBLOOM -> Items.CACTUS_FLOWER;
			case FARMING_WISDOM -> Items.EXPERIENCE_BOTTLE;
			case PEST_COOLDOWN -> Items.CLOCK;
			case YAW -> Items.COMPASS;
			case PITCH -> Items.SPYGLASS;
		};

		return icons.computeIfAbsent(item, ItemIcons::of);
	}

	private static Item cropItem(@Nullable String crop) {
		return switch (crop == null ? "" : crop.toLowerCase(Locale.ROOT)) {
			case "wheat" -> Items.WHEAT;
			case "carrot" -> Items.CARROT;
			case "potato" -> Items.POTATO;
			case "pumpkin" -> Items.PUMPKIN;
			case "melon" -> Items.MELON_SLICE;
			case "cocoa beans" -> Items.COCOA_BEANS;
			case "sugar cane" -> Items.SUGAR_CANE;
			case "cactus" -> Items.CACTUS;
			case "mushroom" -> Items.RED_MUSHROOM;
			case "nether wart" -> Items.NETHER_WART;
			case "sunflower" -> Items.SUNFLOWER;
			case "moonflower" -> Items.BLUE_ORCHID;
			case "wild rose" -> Items.ROSE_BUSH;
			default -> Items.WHEAT_SEEDS;
		};
	}
}
