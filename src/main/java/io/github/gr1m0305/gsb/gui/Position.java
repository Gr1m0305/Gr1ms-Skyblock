package io.github.gr1m0305.gsb.gui;

import com.google.gson.annotations.Expose;

/** Where a GUI element is drawn. The coordinates are the top left corner in scaled screen pixels. */
public class Position {
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 4f;

	@Expose
	public int x;
	@Expose
	public int y;
	@Expose
	public float scale = 1f;
	/** In the horizontal middle of the screen, whatever its width. {@link #x} is not used then. */
	@Expose
	public boolean centered;

	// Used by Gson.
	public Position() {
	}

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/** A position in the horizontal middle of the screen. */
	public static Position centered(int y, float scale) {
		Position position = new Position(0, y);
		position.scale = scale;
		position.centered = true;
		return position;
	}

	public void copyFrom(Position other) {
		x = other.x;
		y = other.y;
		scale = other.scale;
		centered = other.centered;
	}

	public void setScale(float scale) {
		// Steps of 0.05 keep the saved value readable.
		this.scale = Math.round(Math.clamp(scale, MIN_SCALE, MAX_SCALE) * 20) / 20f;
	}

	/** Repairs values a hand-edited config file might contain. */
	public void sanitize() {
		if (Float.isNaN(scale) || scale <= 0) {
			scale = 1f;
		}

		scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
	}
}
