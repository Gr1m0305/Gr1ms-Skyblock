package io.github.gr1m0305.gsb.config.categories;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.data.PriceSource;
import io.github.gr1m0305.gsb.features.farming.FarmingProfitStat;
import io.github.gr1m0305.gsb.features.farming.FarmingStat;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FarmingCategory {
	@Expose
	@ConfigOption(name = "Farming Profit Tracker", desc = "")
	@Accordion
	public ProfitTracker profitTracker = new ProfitTracker();

	// The tracker used to show nothing but the Farming Fortune. Whoever had that turned on keeps it.
	@Expose
	@SerializedName(value = "statTracker", alternate = "fortuneDisplay")
	@ConfigOption(name = "Farming Stat Tracker", desc = "")
	@Accordion
	public StatTracker statTracker = new StatTracker();

	@Expose
	@ConfigOption(name = "Farming Buff Tracker", desc = "")
	@Accordion
	public BuffTracker buffTracker = new BuffTracker();

	@Expose
	@ConfigOption(name = "Pest Cooldown Alert", desc = "")
	@Accordion
	public PestCooldown pestCooldown = new PestCooldown();

	public static class ProfitTracker {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Tracks the crops you farm, the drops of vacuumed pests and what you sell to NPCs on the Garden.\n"
				+ "§eNeeds Sack Notifications turned on in the SkyBlock settings.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@Expose
		@ConfigOption(name = "Price Source", desc = "The price each item is valued at. Items without that kind of price use the other one.")
		@ConfigEditorDropdown
		public PriceSource priceSource = PriceSource.NPC_SELL;

		@Expose
		@ConfigOption(name = "Items Shown", desc = "How many items are listed, most valuable first.")
		@ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1)
		public int itemsShown = 10;

		@Expose
		@ConfigOption(name = "Stats Shown", desc = "The stats below the items, from top to bottom. Drag them to change their order.\n"
				+ "§eFarming XP is counted from the action bar, which has to show the XP as numbers and not as a percentage.")
		@ConfigEditorDraggableList
		public List<FarmingProfitStat> stats = new ArrayList<>(FarmingProfitStat.DEFAULTS);

		@ConfigOption(name = "Reset Tracker", desc = "Sets all items, coins, the Farming XP and the uptime back to zero.\n§eAlso runs with /gsb resetfarming.")
		@ConfigEditorButton(buttonText = "Reset")
		public Runnable reset = () -> GrimsSkyblock.get().farmingTracker().reset();

		@Expose
		public Position position = defaultPosition();

		/** Below the Farming Stat Tracker. */
		public static Position defaultPosition() {
			return new Position(10, 100);
		}
	}

	public static class StatTracker {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Shows the farming stats you pick below on the Garden.\n"
				+ "§eAll but Yaw and Pitch are read from the tab list, so they have to be in its Stats and Pests widgets.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@Expose
		@ConfigOption(name = "Stats Shown", desc = "The stats in the tracker, from top to bottom. Drag them to change their order.\n"
				+ "§eTotal Fortune is your Farming Fortune plus the fortune of the crop.")
		@ConfigEditorDraggableList
		public List<FarmingStat> stats = new ArrayList<>(FarmingStat.DEFAULTS);

		@Expose
		public Position position = defaultPosition();

		public static Position defaultPosition() {
			return new Position(10, 10);
		}
	}

	public static class BuffTracker {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Shows on the Garden how long your pest Farming Fortune bonus still lasts, "
				+ "and how long the spray on the plot you are in does.\n"
				+ "§eReads both from the Pests widget of the tab list.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@Expose
		public Position position = defaultPosition();

		/** At the top, right of the Pet Display. */
		public static Position defaultPosition() {
			return new Position(330, 10);
		}
	}

	public static class PestCooldown {
		private static final float MIN_ALERT_SECONDS = 0.5f;
		private static final float MAX_ALERT_SECONDS = 10f;

		@Expose
		@ConfigOption(name = "Enabled", desc = "Dings and shows \"Pest Cooldown\" on your screen when the pest cooldown runs out.\n"
				+ "§eReads the cooldown from the Pests widget of the tab list.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@Expose
		@ConfigOption(name = "Alert Time", desc = "How many seconds the alert stays on your screen and keeps dinging.")
		@ConfigEditorSlider(minValue = MIN_ALERT_SECONDS, maxValue = MAX_ALERT_SECONDS, minStep = 0.5f)
		public float alertSeconds = 2f;

		@ConfigOption(name = "Test Alert", desc = "Closes this menu and plays the alert once, to check how it looks and sounds.")
		@ConfigEditorButton(buttonText = "Test")
		public Runnable test = () -> GrimsSkyblock.get().pestCooldownAlert().test();

		@Expose
		public Position position = defaultPosition();

		/** Big and in the middle of the screen, a bit below the top. */
		public static Position defaultPosition() {
			return Position.centered(70, 3f);
		}
	}

	public void sanitize() {
		if (profitTracker == null) {
			profitTracker = new ProfitTracker();
		}

		if (statTracker == null) {
			statTracker = new StatTracker();
		}

		if (buffTracker == null) {
			buffTracker = new BuffTracker();
		}

		if (pestCooldown == null) {
			pestCooldown = new PestCooldown();
		}

		if (buffTracker.position == null) {
			buffTracker.position = BuffTracker.defaultPosition();
		}

		if (profitTracker.priceSource == null) {
			profitTracker.priceSource = PriceSource.NPC_SELL;
		}

		if (profitTracker.position == null) {
			profitTracker.position = ProfitTracker.defaultPosition();
		}

		if (profitTracker.stats == null) {
			profitTracker.stats = new ArrayList<>(FarmingProfitStat.DEFAULTS);
		}

		if (statTracker.stats == null) {
			statTracker.stats = new ArrayList<>(FarmingStat.DEFAULTS);
		}

		// A stat this version does not know is read as null, and listing one twice makes no sense. The menu
		// adds to and removes from these very lists, so they have to be ones that can change.
		profitTracker.stats = new ArrayList<>(new LinkedHashSet<>(profitTracker.stats));
		profitTracker.stats.removeIf(Objects::isNull);
		statTracker.stats = new ArrayList<>(new LinkedHashSet<>(statTracker.stats));
		statTracker.stats.removeIf(Objects::isNull);

		if (statTracker.position == null) {
			statTracker.position = StatTracker.defaultPosition();
		}

		if (pestCooldown.position == null) {
			pestCooldown.position = PestCooldown.defaultPosition();
		}

		profitTracker.itemsShown = Math.clamp(profitTracker.itemsShown, 1, 30);
		// The number can also be typed into the box next to the slider, where anything goes.
		pestCooldown.alertSeconds = Float.isNaN(pestCooldown.alertSeconds) ? 2f
				: Math.clamp(pestCooldown.alertSeconds, PestCooldown.MIN_ALERT_SECONDS, PestCooldown.MAX_ALERT_SECONDS);
		profitTracker.position.sanitize();
		statTracker.position.sanitize();
		buffTracker.position.sanitize();
		pestCooldown.position.sanitize();
	}
}
