package io.github.gr1m0305.gsb.gui;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Lays out and draws a {@link HudElement}. Elements with a title get the look all GUI elements of the mod
 * share: a dark panel with a yellow border, the title set into the top of that border and the lines inside.
 */
public final class HudRenderer {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int ACCENT = 0xFFFFFF55;
	private static final int BACKGROUND = 0xC00C0C0C;
	private static final String TITLE_FORMAT = "§e§l";

	private static final int ICON_SIZE = 16;
	/** Between an icon and the text behind it. */
	private static final int ICON_GAP = 2;
	/** The letters of the font are this tall, a line of it is one pixel more. */
	private static final int LETTER_HEIGHT = 8;
	private static final int LINE_GAP = 2;
	private static final int COLUMN_GAP = 6;

	// Where things are in a panel, counted from its top left corner.
	private static final int PANEL_LEFT = 4;
	private static final int PANEL_RIGHT = 6;
	private static final int PANEL_TOP = 11;
	private static final int PANEL_BOTTOM = 3;
	/** Lines without an icon start a little further in than the icons do. */
	private static final int TEXT_INSET = 2;
	private static final int TITLE_X = 8;
	private static final int TITLE_Y = 2;
	/** Between the title and the border on both of its sides. */
	private static final int TITLE_GAP = 2;
	/** The top border runs through the middle of the title. */
	private static final int BORDER_TOP = 5;
	/** How much of the top border is left of the title, and at least right of it. */
	private static final int BORDER_STUB = 4;

	/**
	 * The unscaled size of an element and the widths of its table columns.
	 *
	 * @param title the title in the border of the panel, or null for an element that is drawn without a panel
	 */
	public record Layout(int width, int height, int[] columnWidths, @Nullable Component title) {
	}

	private HudRenderer() {
	}

	/**
	 * @param title see {@link HudElement#title()}
	 */
	public static Layout layout(Font font, @Nullable String title, List<HudLine> lines) {
		boolean panel = title != null;
		int columns = 0;

		for (HudLine line : lines) {
			if (line.isRow()) {
				columns = Math.max(columns, line.cells().size());
			}
		}

		int[] columnWidths = new int[columns];

		for (HudLine line : lines) {
			if (line.isRow()) {
				for (int i = 0; i < line.cells().size(); i++) {
					columnWidths[i] = Math.max(columnWidths[i], font.width(line.cells().get(i)));
				}
			}
		}

		int tableWidth = COLUMN_GAP * Math.max(0, columns - 1);

		for (int columnWidth : columnWidths) {
			tableWidth += columnWidth;
		}

		int width = 0;
		int height = 0;

		for (HudLine line : lines) {
			int textWidth = line.isRow() ? tableWidth : line.cells().isEmpty() ? 0 : font.width(line.cells().getFirst());
			width = Math.max(width, textX(line, panel) + textWidth);
			height += lineHeight(font, line) + LINE_GAP;
		}

		height = Math.max(0, height - LINE_GAP);

		if (!panel) {
			return new Layout(width, height, columnWidths, null);
		}

		Component titleText = LegacyText.parse(TITLE_FORMAT + title);
		// A panel is never narrower than its title, the border around it and the two pixels right of that border.
		int titleWidth = TITLE_X + font.width(titleText) + TITLE_GAP + BORDER_STUB + 2;
		return new Layout(Math.max(width + PANEL_RIGHT, titleWidth), PANEL_TOP + height + PANEL_BOTTOM, columnWidths, titleText);
	}

	private static int textX(HudLine line, boolean panel) {
		int left = panel ? PANEL_LEFT : 0;

		if (line.icon() != null) {
			return left + ICON_SIZE + ICON_GAP;
		}

		return panel ? left + TEXT_INSET : left;
	}

	private static int lineHeight(Font font, HudLine line) {
		return line.icon() != null ? ICON_SIZE : font.lineHeight;
	}

	/** Draws the lines with their top left corner at {@code x}, {@code y}. */
	public static void draw(GuiGraphicsExtractor graphics, Font font, List<HudLine> lines, Layout layout, float x, float y, float scale) {
		boolean panel = layout.title() != null;
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		int lineY = 0;

		if (panel) {
			drawPanel(graphics, font, layout);
			lineY = PANEL_TOP;
		}

		for (HudLine line : lines) {
			int textX = textX(line, panel);
			int textY = lineY;

			if (line.icon() != null) {
				graphics.fakeItem(line.icon(), textX - ICON_GAP - ICON_SIZE, lineY);
				textY += (ICON_SIZE - LETTER_HEIGHT) / 2;
			}

			for (int i = 0; i < line.cells().size(); i++) {
				// On a panel the text is readable as it is, on the bare screen it needs its shadow.
				graphics.text(font, line.cells().get(i), textX, textY, WHITE, !panel);

				if (line.isRow()) {
					textX += layout.columnWidths()[i] + COLUMN_GAP;
				}
			}

			lineY += lineHeight(font, line) + LINE_GAP;
		}

		graphics.pose().popMatrix();
	}

	private static void drawPanel(GuiGraphicsExtractor graphics, Font font, Layout layout) {
		int width = layout.width();
		int height = layout.height();
		int right = width - 2;
		int bottom = height - 2;
		// Background and border both leave their corners out, which rounds the panel off.
		graphics.fill(1, 0, width - 1, height, BACKGROUND);
		graphics.fill(0, 1, 1, height - 1, BACKGROUND);
		graphics.fill(width - 1, 1, width, height - 1, BACKGROUND);

		int titleEnd = TITLE_X + font.width(layout.title()) + TITLE_GAP;
		graphics.fill(TITLE_X - TITLE_GAP - BORDER_STUB, BORDER_TOP, TITLE_X - TITLE_GAP, BORDER_TOP + 1, ACCENT);
		graphics.fill(titleEnd, BORDER_TOP, right, BORDER_TOP + 1, ACCENT);
		graphics.fill(2, bottom, right, bottom + 1, ACCENT);
		graphics.fill(1, BORDER_TOP + 1, 2, bottom, ACCENT);
		graphics.fill(right, BORDER_TOP + 1, right + 1, bottom, ACCENT);
		graphics.text(font, layout.title(), TITLE_X, TITLE_Y, WHITE, false);
	}
}
