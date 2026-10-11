package io.github.gr1m0305.gsb.features.farming;

import java.util.HashMap;
import java.util.Map;

/** Everything the Farming Profit Tracker remembers between sessions. */
public class FarmingTrackerData {
	/** SkyBlock item id to the amount gained. */
	public Map<String, Long> items = new HashMap<>();
	public long pestsKilled;
	public double npcCoins;
	public long npcItemsSold;
	public long uptimeMillis;
}
