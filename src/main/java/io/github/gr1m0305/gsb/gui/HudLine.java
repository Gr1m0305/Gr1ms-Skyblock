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
 * @param bar  the progress bar drawn below the text, or null for a line without one
 */
public record HudLine(@Nullable ItemStack icon, List<Component> cells, @Nullable Bar bar) {
	/**
	 * @param progress how much of the bar is filled, from 0 to 1
	 * @param text     what is written on the bar
	 */
	public record Bar(float progress, Component text) {
	}

	/** A plain line, written with formatting codes. */
	public static HudLine of(String text) {
		return of(LegacyText.parse(text));
	}

	public static HudLine of(Component text) {
		return new HudLine(null, List.of(text), null);
	}

	/** A table row, each cell written with formatting codes. */
	public static HudLine row(String... cells) {
		return new HudLine(null, Arrays.stream(cells).map(LegacyText::parse).toList(), null);
	}

	/** The same line with an item in front of it. */
	public HudLine withIcon(ItemStack icon) {
		return new HudLine(icon, cells, bar);
	}

	/**
	 * The same line with a progress bar below its text.
	 *
	 * @param progress how much of the bar is filled, from 0 to 1
	 * @param text     what is written on the bar, with formatting codes
	 */
	public HudLine withBar(float progress, String text) {
		return new HudLine(icon, cells, new Bar(Math.clamp(progress, 0f, 1f), LegacyText.parse(text)));
	}

	public boolean isRow() {
		return cells.size() > 1;
	}
}
