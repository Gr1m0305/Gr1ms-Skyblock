package io.github.gr1m0305.gsb.gui;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/** Something a feature draws on the screen. It can be moved and resized in the GUI editor. */
public abstract class HudElement {
	private final String name;
	private final Supplier<Position> defaultPosition;

	/**
	 * @param defaultPosition makes the position the element starts at and goes back to when it is reset
	 */
	protected HudElement(String name, Supplier<Position> defaultPosition) {
		this.name = name;
		this.defaultPosition = defaultPosition;
	}

	/** The name shown in the GUI editor. */
	public String name() {
		return name;
	}

	/**
	 * The title in the border of the panel the element is drawn on. An element that returns null is drawn as
	 * bare text instead, which is meant for alerts and not for anything that stays on the screen.
	 */
	public @Nullable String title() {
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
		position().copyFrom(defaultPosition.get());
	}
}
