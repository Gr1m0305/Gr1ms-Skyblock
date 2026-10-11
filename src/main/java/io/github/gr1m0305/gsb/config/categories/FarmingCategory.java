package io.github.gr1m0305.gsb.config.categories;

import com.google.gson.annotations.Expose;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.data.PriceSource;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FarmingCategory {
	@Expose
	@ConfigOption(name = "Farming Profit Tracker", desc = "")
	@Accordion
	public ProfitTracker profitTracker = new ProfitTracker();

	@Expose
	@ConfigOption(name = "Farming Fortune", desc = "")
	@Accordion
	public FortuneDisplay fortuneDisplay = new FortuneDisplay();

	public static class ProfitTracker {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Tracks the crops you farm, the drops of vacuumed pests and what you sell to NPCs on the Garden.\n"
				+ "§eNeeds Sack Notifications turned on in the SkyBlock settings.")
		@ConfigEditorBoolean
		public boolean enabled = true;

		@Expose
		@ConfigOption(name = "Price Source", desc = "The price each item is valued at. Items without that kind of price use the other one.")
		@ConfigEditorDropdown
		public PriceSource priceSource = PriceSource.NPC_SELL;

		@Expose
		@ConfigOption(name = "Items Shown", desc = "How many items are listed, most valuable first.")
		@ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1)
		public int itemsShown = 10;

		@ConfigOption(name = "Reset Tracker", desc = "Sets all items, coins and the uptime back to zero.\n§eAlso runs with /gsb resetfarming.")
		@ConfigEditorButton(buttonText = "Reset")
		public Runnable reset = () -> GrimsSkyblock.get().farmingTracker().reset();

		@Expose
		public Position position = new Position(10, 80);
	}

	public static class FortuneDisplay {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Shows Farming Fortune plus crop fortune on the Garden.\n"
				+ "§eReads both from the Stats widget of the tab list.")
		@ConfigEditorBoolean
		public boolean enabled = true;

		@Expose
		public Position position = new Position(10, 60);
	}

	public void sanitize() {
		if (profitTracker == null) {
			profitTracker = new ProfitTracker();
		}

		if (fortuneDisplay == null) {
			fortuneDisplay = new FortuneDisplay();
		}

		if (profitTracker.priceSource == null) {
			profitTracker.priceSource = PriceSource.NPC_SELL;
		}

		if (profitTracker.position == null) {
			profitTracker.position = new Position(10, 80);
		}

		if (fortuneDisplay.position == null) {
			fortuneDisplay.position = new Position(10, 60);
		}

		profitTracker.itemsShown = Math.clamp(profitTracker.itemsShown, 1, 30);
		profitTracker.position.sanitize();
		fortuneDisplay.position.sanitize();
	}
}
