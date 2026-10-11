package io.github.gr1m0305.gsb.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.compat.GameCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** The GUI editor: drag the elements of all enabled features around and scroll to resize them. */
public class PositionEditorScreen extends Screen {
	private static final int BOX_PADDING = 2;
	private static final float SCALE_STEP = 0.1f;
	private static final int WHITE = 0xFFFFFFFF;

	private record Placed(HudElement element, List<HudLine> lines, HudRenderer.Layout layout, HudManager.Placement placement) {
	}

	private final @Nullable Screen parent;
	private @Nullable HudElement dragging;
	private @Nullable HudElement selected;
	private double grabOffsetX;
	private double grabOffsetY;

	public PositionEditorScreen(@Nullable Screen parent) {
		super(Component.literal("Gr1m's Skyblock GUI Editor"));
		this.parent = parent;
	}

	/** Opens the editor on top of whatever screen is open right now and returns to it afterwards. */
	public static void open() {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new PositionEditorScreen(GameCompat.screen(minecraft)));
	}

	private List<Placed> placeElements() {
		List<Placed> placed = new ArrayList<>();

		for (HudElement element : HudManager.elements()) {
			if (!element.isEnabled()) {
				continue;
			}

			List<HudLine> lines = element.previewLines();

			if (lines.isEmpty()) {
				continue;
			}

			HudRenderer.Layout layout = HudRenderer.layout(font, lines);
			placed.add(new Placed(element, lines, layout, HudManager.place(element.position(), layout, width, height)));
		}

		return placed;
	}

	private @Nullable Placed elementAt(List<Placed> placed, double x, double y) {
		// Later elements are drawn on top, so they win.
		for (int i = placed.size() - 1; i >= 0; i--) {
			HudManager.Placement placement = placed.get(i).placement();

			if (x >= placement.x() - BOX_PADDING && x < placement.x() + placement.width() + BOX_PADDING
					&& y >= placement.y() - BOX_PADDING && y < placement.y() + placement.height() + BOX_PADDING) {
				return placed.get(i);
			}
		}

		return null;
	}

	private @Nullable Placed find(List<Placed> placed, @Nullable HudElement element) {
		for (Placed candidate : placed) {
			if (candidate.element() == element) {
				return candidate;
			}
		}

		return null;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
		// No blur here, the point is to see where things end up on the screen.
		graphics.fill(0, 0, width, height, 0x60000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
		super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
		List<Placed> placed = placeElements();
		Placed active = dragging != null ? find(placed, dragging) : elementAt(placed, mouseX, mouseY);

		if (active == null) {
			active = find(placed, selected);
		}

		for (Placed entry : placed) {
			HudManager.Placement placement = entry.placement();
			int left = (int) Math.floor(placement.x()) - BOX_PADDING;
			int top = (int) Math.floor(placement.y()) - BOX_PADDING;
			int boxWidth = (int) Math.ceil(placement.width()) + BOX_PADDING * 2;
			int boxHeight = (int) Math.ceil(placement.height()) + BOX_PADDING * 2;
			graphics.fill(left, top, left + boxWidth, top + boxHeight, entry == active ? 0xA0404040 : 0x80202020);
			graphics.outline(left, top, boxWidth, boxHeight, entry == active ? 0xFFFFFF55 : 0x80FFFFFF);
			HudRenderer.draw(graphics, font, entry.lines(), entry.layout(), placement.x(), placement.y(), entry.element().position().scale);
		}

		int centerX = width / 2;
		graphics.centeredText(font, LegacyText.parse("§e§lGUI Editor"), centerX, 8, WHITE);

		if (placed.isEmpty()) {
			graphics.centeredText(font, LegacyText.parse("§cNo features with a GUI element are enabled. Turn one on in /gsb first."), centerX, 22, WHITE);
			return;
		}

		graphics.centeredText(font, LegacyText.parse("§7Drag to move §8| §7Scroll to resize §8| §7Right-click to reset §8| §7Arrow keys to nudge"), centerX, 20, WHITE);

		if (active != null) {
			Position position = active.element().position();
			String info = "§e" + active.element().name() + " §7x: §f" + Math.round(active.placement().x()) + " §7y: §f" + Math.round(active.placement().y())
					+ " §7size: §f" + Math.round(position.scale * 100) + "%";
			graphics.centeredText(font, LegacyText.parse(info), centerX, 32, WHITE);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Placed target = elementAt(placeElements(), event.x(), event.y());

		if (target == null) {
			selected = null;
			return super.mouseClicked(event, doubleClick);
		}

		selected = target.element();

		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = target.element();
			grabOffsetX = event.x() - target.placement().x();
			grabOffsetY = event.y() - target.placement().y();
		} else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			target.element().resetPosition();
		}

		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}

		moveTo(dragging, event.x() - grabOffsetX, event.y() - grabOffsetY);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = null;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Placed target = elementAt(placeElements(), mouseX, mouseY);
		HudElement element = target != null ? target.element() : selected;

		if (element == null || scrollY == 0) {
			return false;
		}

		selected = element;
		resize(element, scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		HudElement element = selected;

		if (element != null) {
			int step = event.hasShiftDown() ? 10 : 1;
			Placed placed = find(placeElements(), element);

			if (placed != null) {
				float x = placed.placement().x();
				float y = placed.placement().y();

				switch (event.key()) {
					case InputConstants.KEY_LEFT -> moveTo(element, x - step, y);
					case InputConstants.KEY_RIGHT -> moveTo(element, x + step, y);
					case InputConstants.KEY_UP -> moveTo(element, x, y - step);
					case InputConstants.KEY_DOWN -> moveTo(element, x, y + step);
					case InputConstants.KEY_EQUALS -> resize(element, SCALE_STEP);
					case InputConstants.KEY_MINUS -> resize(element, -SCALE_STEP);
					case InputConstants.KEY_R -> element.resetPosition();
					default -> {
						return super.keyPressed(event);
					}
				}

				return true;
			}
		}

		return super.keyPressed(event);
	}

	private void moveTo(HudElement element, double x, double y) {
		Placed placed = find(placeElements(), element);

		if (placed == null) {
			return;
		}

		Position position = element.position();
		position.x = (int) Math.round(Math.clamp(x, 0, Math.max(0, width - placed.placement().width())));
		position.y = (int) Math.round(Math.clamp(y, 0, Math.max(0, height - placed.placement().height())));
	}

	private void resize(HudElement element, float change) {
		Position position = element.position();
		position.setScale(position.scale + change);
		Placed placed = find(placeElements(), element);

		// A bigger element may no longer fit where it was.
		if (placed != null) {
			position.x = Math.round(placed.placement().x());
			position.y = Math.round(placed.placement().y());
		}
	}

	@Override
	public void onClose() {
		GrimsSkyblock.get().config().save();
		GameCompat.setScreen(minecraft, parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
