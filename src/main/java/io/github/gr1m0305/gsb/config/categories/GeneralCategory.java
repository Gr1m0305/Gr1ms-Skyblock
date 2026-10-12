package io.github.gr1m0305.gsb.config.categories;

import com.google.gson.annotations.Expose;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GeneralCategory {
	@Expose
	@ConfigOption(name = "Pet Display", desc = "")
	@Accordion
	public PetDisplay petDisplay = new PetDisplay();

	public static class PetDisplay {
		@Expose
		@ConfigOption(name = "Enabled", desc = "Shows your pet with its level and how far it is to the next one, everywhere on SkyBlock.\n"
				+ "§eReads the pet from the Pet widget of the tab list.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@Expose
		public Position position = defaultPosition();

		/** At the top, right of the Farming Stat Tracker. */
		public static Position defaultPosition() {
			return new Position(170, 10);
		}
	}

	public void sanitize() {
		if (petDisplay == null) {
			petDisplay = new PetDisplay();
		}

		if (petDisplay.position == null) {
			petDisplay.position = PetDisplay.defaultPosition();
		}

		petDisplay.position.sanitize();
	}
}
