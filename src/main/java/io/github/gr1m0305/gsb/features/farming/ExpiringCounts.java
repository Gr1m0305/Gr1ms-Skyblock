package io.github.gr1m0305.gsb.features.farming;

import java.util.HashMap;
import java.util.Map;

/** Item amounts that are only valid for a short time after they were added. */
final class ExpiringCounts {
	private static final class Entry {
		long amount;
		long expiresAt;
	}

	private final long lifetimeMillis;
	private final Map<String, Entry> entries = new HashMap<>();

	ExpiringCounts(long lifetimeMillis) {
		this.lifetimeMillis = lifetimeMillis;
	}

	void add(String id, long amount, long now) {
		Entry entry = entries.get(id);

		if (entry == null || entry.expiresAt < now) {
			entry = new Entry();
			entries.put(id, entry);
		}

		entry.amount += amount;
		entry.expiresAt = now + lifetimeMillis;
	}

	/** Takes up to {@code max} of the item out and returns how many were taken. */
	long consume(String id, long max, long now) {
		Entry entry = entries.get(id);

		if (entry == null) {
			return 0;
		}

		if (entry.expiresAt < now) {
			entries.remove(id);
			return 0;
		}

		long taken = Math.min(max, entry.amount);
		entry.amount -= taken;

		if (entry.amount <= 0) {
			entries.remove(id);
		}

		return taken;
	}

	void clear() {
		entries.clear();
	}
}
