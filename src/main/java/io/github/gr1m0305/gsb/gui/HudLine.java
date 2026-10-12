package io.github.gr1m0305.gsb.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One line of a GUI element. A line with several cells is a table row, the cells of all table rows of an
 * element are lined up in columns.
 *
 * @param icon the item drawn in front of the line, or null for a line that is only text
 */
public record HudLine(@Nullable ItemStack icon, List<Component> cells) {
	/** A plain line, written with formatting codes. */
	public static HudLine of(String text) {
		return new HudLine(null, List.of(LegacyText.parse(text)));
	}

	/** A table row, each cell written with formatting codes. */
	public static HudLine row(String... cells) {
		return new HudLine(null, Arrays.stream(cells).map(LegacyText::parse).toList());
	}

	/** The same line with an item in front of it. */
	public HudLine withIcon(ItemStack icon) {
		return new HudLine(icon, cells);
	}

	public boolean isRow() {
		return cells.size() > 1;
	}
}
