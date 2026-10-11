package io.github.gr1m0305.gsb.features.farming;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the pest cooldown out of the Pests widget of the tab list. */
public final class PestCooldownParser {
	private static final String PREFIX = "Cooldown: ";
	// 1m 58s, 2m, 58s
	private static final Pattern TIME = Pattern.compile("^(?:(?<minutes>\\d+)m)?\\s*(?:(?<seconds>\\d+)s)?$");

	public enum State {
		/** The cooldown is running. */
		COUNTING,
		/** The cooldown is over, the next pest can spawn. */
		READY,
		/** The tab list does not say, for example because it shows MAX PESTS instead. */
		UNKNOWN
	}

	/**
	 * @param seconds what is left of the cooldown, only meaningful while it is {@link State#COUNTING}
	 */
	public record Cooldown(State state, int seconds) {
		public static final Cooldown READY = new Cooldown(State.READY, 0);
		public static final Cooldown UNKNOWN = new Cooldown(State.UNKNOWN, 0);

		public static Cooldown counting(int seconds) {
			return new Cooldown(State.COUNTING, seconds);
		}
	}

	private PestCooldownParser() {
	}

	public static Cooldown parse(Collection<String> tabLines) {
		for (String line : tabLines) {
			String trimmed = line.trim();

			if (!trimmed.startsWith(PREFIX)) {
				continue;
			}

			String value = trimmed.substring(PREFIX.length()).trim();

			if (value.startsWith("READY")) {
				return Cooldown.READY;
			}

			Matcher matcher = TIME.matcher(value);

			if (!matcher.matches() || (matcher.group("minutes") == null && matcher.group("seconds") == null)) {
				return Cooldown.UNKNOWN;
			}

			int seconds = 0;

			if (matcher.group("minutes") != null) {
				seconds += Integer.parseInt(matcher.group("minutes")) * 60;
			}

			if (matcher.group("seconds") != null) {
				seconds += Integer.parseInt(matcher.group("seconds"));
			}

			return seconds > 0 ? Cooldown.counting(seconds) : Cooldown.READY;
		}

		return Cooldown.UNKNOWN;
	}
}
