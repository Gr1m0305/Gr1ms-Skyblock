package io.github.gr1m0305.gsb.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * One line of a GUI element. A line with several cells is a table row, the cells of all table rows of an
 * element are lined up in columns.
 */
public record HudLine(List<Component> cells) {
	/** A plain line, written with formatting codes. */
	public static HudLine of(String text) {
		return new HudLine(List.of(LegacyText.parse(text)));
	}

	/** A table row, each cell written with formatting codes. */
	public static HudLine row(String... cells) {
		return new HudLine(Arrays.stream(cells).map(LegacyText::parse).toList());
	}

	public boolean isRow() {
		return cells.size() > 1;
	}
}
