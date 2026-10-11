package io.github.gr1m0305.gsb.gui;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Lays out and draws the lines of a {@link HudElement}. */
public final class HudRenderer {
	public static final int LINE_HEIGHT = 10;
	private static final int ROW_INDENT = 4;
	private static final int COLUMN_GAP = 6;
	private static final int WHITE = 0xFFFFFFFF;

	/** The unscaled size of an element and the widths of its table columns. */
	public record Layout(int width, int height, int[] columnWidths) {
	}

	private HudRenderer() {
	}

	public static Layout layout(Font font, List<HudLine> lines) {
		int columns = 0;

		for (HudLine line : lines) {
			if (line.isRow()) {
				columns = Math.max(columns, line.cells().size());
			}
		}

		int[] columnWidths = new int[columns];
		int width = 0;

		for (HudLine line : lines) {
			if (line.isRow()) {
				for (int i = 0; i < line.cells().size(); i++) {
					columnWidths[i] = Math.max(columnWidths[i], font.width(line.cells().get(i)));
				}
			} else if (!line.cells().isEmpty()) {
				width = Math.max(width, font.width(line.cells().getFirst()));
			}
		}

		if (columns > 0) {
			int rowWidth = ROW_INDENT + COLUMN_GAP * (columns - 1);

			for (int columnWidth : columnWidths) {
				rowWidth += columnWidth;
			}

			width = Math.max(width, rowWidth);
		}

		return new Layout(width, lines.size() * LINE_HEIGHT, columnWidths);
	}

	/** Draws the lines with their top left corner at {@code x}, {@code y}. */
	public static void draw(GuiGraphicsExtractor graphics, Font font, List<HudLine> lines, Layout layout, float x, float y, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		int lineY = 0;

		for (HudLine line : lines) {
			if (line.isRow()) {
				int cellX = ROW_INDENT;

				for (int i = 0; i < line.cells().size(); i++) {
					graphics.text(font, line.cells().get(i), cellX, lineY, WHITE, true);
					cellX += layout.columnWidths()[i] + COLUMN_GAP;
				}
			} else if (!line.cells().isEmpty()) {
				Component text = line.cells().getFirst();
				graphics.text(font, text, 0, lineY, WHITE, true);
			}

			lineY += LINE_HEIGHT;
		}

		graphics.pose().popMatrix();
	}
}
