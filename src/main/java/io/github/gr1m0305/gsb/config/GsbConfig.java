package io.github.gr1m0305.gsb.config;

import java.util.List;

import com.google.gson.annotations.Expose;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.config.categories.GuiCategory;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.Social;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

/** The root of the config menu. Every feature lives in the category it belongs to. */
public class GsbConfig extends Config {
	/** The buttons in the top right corner of the menu. */
	private static final List<Social> SOCIALS = List.of(new GitHubLink());

	@Expose
	@Category(name = "GUI", desc = "Change where this mod draws things on your screen.")
	public GuiCategory gui = new GuiCategory();

	@Expose
	@Category(name = "Farming", desc = "Features for farming on the Garden.")
	public FarmingCategory farming = new FarmingCategory();

	@Override
	public StructuredText getTitle() {
		return StructuredText.of("Gr1m's Skyblock " + GrimsSkyblock.version() + " by §cGr1m0305");
	}

	@Override
	public List<Social> getSocials() {
		return SOCIALS;
	}

	@Override
	public void saveNow() {
		GrimsSkyblock.get().config().save();
	}

	/** Repairs what a hand-edited config file can break. */
	public void sanitize() {
		if (gui == null) {
			gui = new GuiCategory();
		}

		if (farming == null) {
			farming = new FarmingCategory();
		}

		farming.sanitize();
	}
}
