package io.github.gr1m0305.gsb.features.farming;

import java.util.List;

/** What the Farming Profit Tracker can show below its items. */
public enum FarmingProfitStat {
	TOTAL_PROFIT("Total Profit"),
	PROFIT_PER_HOUR("Profit Per Hour"),
	UPTIME("Total Uptime"),
	PESTS_KILLED("Pests Killed"),
	NPC_COINS("Sold to NPC"),
	FARMING_XP("Farming XP"),
	FARMING_XP_PER_HOUR("Farming XP Per Hour");

	/** What the tracker shows until the player picks something else. */
	public static final List<FarmingProfitStat> DEFAULTS = List.of(TOTAL_PROFIT, PROFIT_PER_HOUR, UPTIME);

	private final String displayName;

	FarmingProfitStat(String displayName) {
		this.displayName = displayName;
	}

	// The config menu lists the stats by this.
	@Override
	public String toString() {
		return displayName;
	}
}
