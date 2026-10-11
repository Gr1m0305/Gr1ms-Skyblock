package io.github.gr1m0305.gsb.features.farming;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;

/** Shows the Farming Fortune and the crop fortune from the tab list added together. */
public final class FarmingFortuneDisplay extends HudElement {
	private static final String LABEL = "§eFarming Fortune: ";

	private final Supplier<FarmingCategory.FortuneDisplay> config;
	private List<String> parsedTabLines = List.of();
	private Optional<FarmingFortuneParser.Fortune> fortune = Optional.empty();
	private List<HudLine> lines = List.of(HudLine.of(LABEL + "§cnot in tab list"));

	public FarmingFortuneDisplay(Supplier<FarmingCategory.FortuneDisplay> config) {
		super("Farming Fortune", FarmingCategory.FortuneDisplay::defaultPosition);
		this.config = config;
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

	private Optional<FarmingFortuneParser.Fortune> fortune() {
		List<String> tabLines = SkyblockState.tabLines();

		// The tab list is read into a new list a few times per second, only parse it when that happened.
		if (tabLines != parsedTabLines) {
			parsedTabLines = tabLines;
			fortune = FarmingFortuneParser.parse(tabLines);
			lines = List.of(HudLine.of(fortune
					.map(value -> LABEL + "§6☘" + value.total())
					.orElse(LABEL + "§cnot in tab list")));
		}

		return fortune;
	}

	@Override
	public List<HudLine> lines() {
		fortune();
		return lines;
	}

	@Override
	public List<HudLine> previewLines() {
		return fortune().isPresent() ? lines() : List.of(HudLine.of(LABEL + "§6☘1724"));
	}
}
