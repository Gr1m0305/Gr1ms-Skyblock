package io.github.gr1m0305.gsb.features.farming;

import java.util.List;

/** What the Farming Stat Tracker can show. */
public enum FarmingStat {
	/** Farming Fortune and the fortune of the crop added together, which is what the crop is farmed with. */
	TOTAL_FORTUNE("Total Fortune"),
	FARMING_FORTUNE("Farming Fortune"),
	CROP_FORTUNE("Crop Fortune"),
	SPEED("Speed"),
	BONUS_PEST_CHANCE("Bonus Pest Chance"),
	FARMING_WISDOM("Farming Wisdom"),
	PEST_COOLDOWN("Pest Cooldown"),
	YAW("Yaw"),
	PITCH("Pitch");

	/** What the tracker shows until the player picks something else. */
	public static final List<FarmingStat> DEFAULTS = List.of(TOTAL_FORTUNE, SPEED, YAW, PITCH);

	private final String displayName;

	FarmingStat(String displayName) {
		this.displayName = displayName;
	}

	// The config menu lists the stats by this.
	@Override
	public String toString() {
		return displayName;
	}
}
