package io.github.gr1m0305.gsb.gui;

import java.util.List;

/** Something a feature draws on the screen. It can be moved and resized in the GUI editor. */
public abstract class HudElement {
	private final String name;
	private final int defaultX;
	private final int defaultY;

	protected HudElement(String name, int defaultX, int defaultY) {
		this.name = name;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
	}

	/** The name shown in the GUI editor. */
	public String name() {
		return name;
	}

	public abstract Position position();

	/** Whether the feature is turned on. Only elements of enabled features show up in the GUI editor. */
	public abstract boolean isEnabled();

	/** Whether the element should be drawn right now. */
	public abstract boolean isVisible();

	public abstract List<HudLine> lines();

	/** What the GUI editor shows, for when {@link #lines()} has nothing to show at the moment. */
	public List<HudLine> previewLines() {
		return lines();
	}

	public void resetPosition() {
		Position position = position();
		position.x = defaultX;
		position.y = defaultY;
		position.scale = 1f;
	}
}
