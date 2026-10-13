package io.github.gr1m0305.gsb.features.farming;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.jspecify.annotations.Nullable;

/**
 * The bookkeeping of the Farming Profit Tracker, kept free of Minecraft classes.
 *
 * <p>Items are counted from two places: changes of the player's inventory while no menu is open, and the
 * "[Sacks]" chat messages. Moving items between those two would count them twice, so such moves are
 * remembered for a moment and cancelled out.
 *
 * <p>Items that go away again are taken off, in the sacks as well as in the inventory. That is what keeps
 * crops from being counted twice when they are crafted into their enchanted form: the enchanted item comes in
 * and the crops it was made of go out.
 */
public final class FarmingTrackerLogic {
	/** How long an item moved into or out of a sack by hand may take to show up in a "[Sacks]" message. */
	static final long SACK_MESSAGE_DELAY_MILLIS = 60_000;
	/** How far apart the "Moved ... from your Sacks" message and the inventory update may be. */
	static final long SACK_WITHDRAWAL_WINDOW_MILLIS = 3_000;
	/** The tracker pauses after this long without a sign of farming. Short breaks, like typing a command, are below it. */
	static final long IDLE_TIMEOUT_MILLIS = 5_000;

	public record ItemRow(String id, long count, double value) {
	}

	/**
	 * @param rows        the most valuable items, best first
	 * @param hiddenItems how many more items there are that did not fit into {@code rows}
	 */
	public record Summary(List<ItemRow> rows, int hiddenItems, double hiddenValue, double totalProfit, double profitPerHour,
			long pestsKilled, double npcCoins, long npcItemsSold, double farmingXp, double farmingXpPerHour, long uptimeMillis) {
	}

	private static final class RecentGain {
		final String id;
		final long time;
		long amount;

		RecentGain(String id, long amount, long time) {
			this.id = id;
			this.amount = amount;
			this.time = time;
		}
	}

	private final ExpiringCounts insertedIntoSacks = new ExpiringCounts(SACK_MESSAGE_DELAY_MILLIS);
	private final ExpiringCounts takenOutOfSacks = new ExpiringCounts(SACK_MESSAGE_DELAY_MILLIS);
	private final ExpiringCounts withdrawnFromSacks = new ExpiringCounts(SACK_WITHDRAWAL_WINDOW_MILLIS);
	private final ArrayDeque<RecentGain> recentInventoryGains = new ArrayDeque<>();
	private FarmingChatParser.@Nullable FarmingXp lastXpShown;
	private FarmingTrackerData data;
	private long lastActivity = Long.MIN_VALUE / 2;
	private long lastGain = Long.MIN_VALUE / 2;
	private boolean dirty;

	public FarmingTrackerLogic(FarmingTrackerData data) {
		this.data = data;
	}

	public FarmingTrackerData data() {
		return data;
	}

	/** Whether there are changes that have not been written to disk, clears the flag. */
	public boolean takeDirty() {
		boolean wasDirty = dirty;
		dirty = false;
		return wasDirty;
	}

	public void reset() {
		data = new FarmingTrackerData();
		insertedIntoSacks.clear();
		takenOutOfSacks.clear();
		withdrawnFromSacks.clear();
		recentInventoryGains.clear();
		dirty = true;
	}

	/** The amount of an item in the inventory changed while no menu was open. */
	public void inventoryChanged(String id, long delta, long now) {
		if (delta > 0) {
			long counted = delta - withdrawnFromSacks.consume(id, delta, now);

			if (counted > 0) {
				addItem(id, counted);
				recentInventoryGains.add(new RecentGain(id, counted, now));
				lastActivity = now;
				lastGain = now;
			}
		} else if (delta < 0) {
			// Compacted, planted or thrown away, either way it is not there to be sold anymore.
			addItem(id, delta);
		}

		pruneRecentGains(now);
	}

	/** The player put items from the inventory into a sack by hand. */
	public void insertedIntoSack(String id, long amount, long now) {
		insertedIntoSacks.add(id, amount, now);
	}

	/**
	 * A "[Sacks]" chat message reported a change.
	 *
	 * @param duringMenu whether a menu of the server was open in the seconds the message is about
	 */
	public void sackChanged(String id, long delta, long now, boolean duringMenu) {
		if (delta > 0) {
			long counted = delta - insertedIntoSacks.consume(id, delta, now);

			if (counted > 0) {
				addItem(id, counted);
				lastActivity = now;
				lastGain = now;
			}
		} else if (delta < 0) {
			long lost = -delta - takenOutOfSacks.consume(id, -delta, now);

			// Crafted into something else or planted again, either way it is not there to be sold anymore. What a
			// menu took is different: that was sold, handed in or moved to the inventory, where a menu counts nothing.
			if (lost > 0 && !duringMenu) {
				addItem(id, -lost);
			}
		}
	}

	/** Chat said items were moved from the sacks to the inventory, which the inventory tracking sees as a gain. */
	public void withdrawnFromSack(String id, long amount, long now) {
		// The "[Sacks]" message that follows reports them as gone, which they are not.
		takenOutOfSacks.add(id, amount, now);
		pruneRecentGains(now);
		long remaining = amount;

		// The inventory update may have arrived before the chat message, take that gain back.
		for (Iterator<RecentGain> iterator = recentInventoryGains.descendingIterator(); iterator.hasNext() && remaining > 0; ) {
			RecentGain gain = iterator.next();

			if (!gain.id.equals(id)) {
				continue;
			}

			long taken = Math.min(remaining, gain.amount);
			gain.amount -= taken;
			remaining -= taken;
			addItem(id, -taken);

			if (gain.amount <= 0) {
				iterator.remove();
			}
		}

		if (remaining > 0) {
			withdrawnFromSacks.add(id, remaining, now);
		}
	}

	public void pestKilled(long now) {
		data.pestsKilled++;
		lastActivity = now;
		dirty = true;
	}

	public void soldToNpc(double coins, long itemCount) {
		data.npcCoins += coins;
		data.npcItemsSold += itemCount;
		dirty = true;
	}

	/** The action bar showed the Farming XP of the player, which it does for a moment whenever some is gained. */
	public void farmingXpShown(FarmingChatParser.FarmingXp shown) {
		FarmingChatParser.FarmingXp last = lastXpShown;
		lastXpShown = shown;

		// A percentage of a level does not say how much XP that is.
		if (!shown.hasNumbers()) {
			return;
		}

		// The action bar names a single gain and is not sent for every one of them, so what counts is how far the
		// XP of the level went up. Only where there is nothing to compare with, at first and after a level-up, it
		// is that one gain.
		boolean sameLevel = last != null && last.hasNumbers() && last.needed().equals(shown.needed()) && shown.current() >= last.current();
		double gained = sameLevel ? shown.current() - last.current() : shown.gained();

		if (gained > 0) {
			data.farmingXp += gained;
			dirty = true;
		}
	}

	/** The tracker stopped watching. XP gained from here on is not counted, the next one shown starts anew. */
	public void farmingXpOutOfSight() {
		lastXpShown = null;
	}

	/** Whether the action bar has the Farming XP as a percentage only, which cannot be counted. */
	public boolean farmingXpIsPercentage() {
		return lastXpShown != null && !lastXpShown.hasNumbers();
	}

	/**
	 * Advances the uptime.
	 *
	 * @param farmingInput whether the player is holding down a mouse button in the world right now
	 */
	public void tick(long now, long elapsedMillis, boolean farmingInput) {
		if (farmingInput) {
			lastActivity = now;
		}

		if (!isPaused(now)) {
			data.uptimeMillis += elapsedMillis;
			dirty = true;
		}
	}

	/**
	 * Whether there was no sign of farming for a few seconds. The uptime stands still then, and with it the
	 * profit per hour. Items are still counted, the last crops reach the sacks after the player stopped.
	 */
	public boolean isPaused(long now) {
		return now - lastActivity > IDLE_TIMEOUT_MILLIS;
	}

	/** Milliseconds since an item was last counted. */
	public long millisSinceLastGain(long now) {
		return now - lastGain;
	}

	public Summary summarize(ToDoubleFunction<String> priceOf, int maxRows) {
		List<ItemRow> rows = new ArrayList<>();
		double totalProfit = 0;

		for (Map.Entry<String, Long> entry : data.items.entrySet()) {
			double value = entry.getValue() * priceOf.applyAsDouble(entry.getKey());
			// Items that went below zero were used up, they lower the profit but are not listed.
			totalProfit += value;

			if (entry.getValue() > 0) {
				rows.add(new ItemRow(entry.getKey(), entry.getValue(), value));
			}
		}

		rows.sort(Comparator.comparingDouble(ItemRow::value).reversed()
				.thenComparing(Comparator.comparingLong(ItemRow::count).reversed())
				.thenComparing(ItemRow::id));

		int shown = Math.min(Math.max(maxRows, 0), rows.size());
		double hiddenValue = 0;

		for (ItemRow row : rows.subList(shown, rows.size())) {
			hiddenValue += row.value();
		}

		double hours = data.uptimeMillis / 3_600_000.0;
		double profitPerHour = data.uptimeMillis >= 1000 ? totalProfit / hours : 0;
		double farmingXpPerHour = data.uptimeMillis >= 1000 ? data.farmingXp / hours : 0;
		return new Summary(List.copyOf(rows.subList(0, shown)), rows.size() - shown, hiddenValue, totalProfit, profitPerHour,
				data.pestsKilled, data.npcCoins, data.npcItemsSold, data.farmingXp, farmingXpPerHour, data.uptimeMillis);
	}

	private void addItem(String id, long delta) {
		if (delta == 0) {
			return;
		}

		long count = data.items.getOrDefault(id, 0L) + delta;

		if (count == 0) {
			data.items.remove(id);
		} else {
			data.items.put(id, count);
		}

		dirty = true;
	}

	private void pruneRecentGains(long now) {
		while (!recentInventoryGains.isEmpty() && now - recentInventoryGains.peekFirst().time > SACK_WITHDRAWAL_WINDOW_MILLIS) {
			recentInventoryGains.removeFirst();
		}
	}
}
