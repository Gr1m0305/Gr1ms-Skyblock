package io.github.gr1m0305.gsb.config.categories;

import io.github.gr1m0305.gsb.gui.PositionEditorScreen;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class GuiCategory {
	@ConfigOption(name = "Edit GUI Locations", desc = "Move and resize the GUI elements of all enabled features.\n"
			+ "§eDrag to move, scroll to resize. Also opens with /gsb gui.")
	@ConfigEditorButton(buttonText = "Edit")
	public Runnable editLocations = PositionEditorScreen::open;
}
