package io.github.gr1m0305.gsb.features.farming;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.ItemIcons;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import io.github.gr1m0305.gsb.util.NumberFormats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Shows how long two buffs still last on the Garden: the Farming Fortune bonus the Pesthunter gives for
 * pests, and the spray on the plot the player is in. Both are read from the tab list.
 */
public final class FarmingBuffTracker extends HudElement {
	private static final String MISSING = "§cnot in tab list";
	private static final FarmingBuffParser.Buffs EXAMPLE = new FarmingBuffParser.Buffs(
			Optional.of(new FarmingBuffParser.PestFortune(200, "29m")), Optional.of(new FarmingBuffParser.Spray("Compost", "12m")));

	private final Supplier<FarmingCategory.BuffTracker> config;
	private @Nullable ItemStack pestFortuneIcon;
	private @Nullable ItemStack sprayIcon;
	private @Nullable List<String> readTabLines;
	private FarmingBuffParser.Buffs buffs = FarmingBuffParser.Buffs.UNKNOWN;
	private List<HudLine> lines = List.of();

	public FarmingBuffTracker(Supplier<FarmingCategory.BuffTracker> config) {
		super("Farming Buff Tracker", FarmingCategory.BuffTracker::defaultPosition);
		this.config = config;
	}

	@Override
	public String title() {
		return "Farming Buffs";
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
		readTabList();
		return lines;
	}

	/** The editor is also open away from the Garden, where the tab list has neither of the two. */
	@Override
	public List<HudLine> previewLines() {
		readTabList();
		return buffs.equals(FarmingBuffParser.Buffs.UNKNOWN) ? buildLines(EXAMPLE) : lines;
	}

	/** The tab list is read into a new list a few times per second, only look at it when that happened. */
	private void readTabList() {
		List<String> tabLines = SkyblockState.tabLines();

		if (tabLines == readTabLines) {
			return;
		}

		readTabLines = tabLines;
		FarmingBuffParser.Buffs read = FarmingBuffParser.parse(tabLines);

		if (lines.isEmpty() || !read.equals(buffs)) {
			buffs = read;
			lines = buildLines(read);
		}
	}

	private List<HudLine> buildLines(FarmingBuffParser.Buffs buffs) {
		if (pestFortuneIcon == null || sprayIcon == null) {
			// The game draws its Pests widget with the hopper minecart too.
			pestFortuneIcon = ItemIcons.of(Items.HOPPER_MINECART);
			sprayIcon = ItemIcons.of(Items.HONEY_BOTTLE);
		}

		String pestFortune = buffs.pestFortune()
				.map(buff -> buff.isActive() ? time(buff.timeLeft()) + " §7(+" + NumberFormats.grouped(buff.fortune()) + "☘)" : "§cInactive")
				.orElse(MISSING);
		String spray = buffs.spray()
				.map(sprayed -> sprayed.isActive() ? time(sprayed.timeLeft()) + " §7(" + sprayed.name() + ")" : "§cNot sprayed")
				.orElse(MISSING);
		return List.of(HudLine.of("§fPest Fortune: " + pestFortune).withIcon(pestFortuneIcon), HudLine.of("§fPlot Spray: " + spray).withIcon(sprayIcon));
	}

	/** A time of less than a minute is red, the buff is about to end. */
	private static String time(String timeLeft) {
		return (timeLeft.contains("m") || timeLeft.contains("h") ? "§e" : "§c") + timeLeft;
	}
}
