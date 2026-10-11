package io.github.gr1m0305.gsb.util;

import java.util.Locale;

public final class NumberFormats {
	private static final String[] SUFFIXES = {"", "k", "M", "B", "T"};

	private NumberFormats() {
	}

	/** {@code 76549} becomes {@code 76,549}. */
	public static String grouped(long value) {
		return String.format(Locale.US, "%,d", value);
	}

	/** Rounds to a whole number and groups it, {@code 557532196.4} becomes {@code 557,532,196}. */
	public static String grouped(double value) {
		return grouped(Math.round(value));
	}

	/** {@code 41900000} becomes {@code 41.9M}, {@code 34000000} becomes {@code 34M}. */
	public static String compact(double value) {
		double abs = Math.abs(value);

		if (abs < 1000) {
			return String.valueOf(Math.round(value));
		}

		int unit = 0;

		while (abs >= 1000 && unit < SUFFIXES.length - 1) {
			abs /= 1000;
			unit++;
		}

		double rounded = Math.round(abs * 10) / 10.0;

		// 999.96k rounds up to 1000k, which should read 1M.
		if (rounded >= 1000 && unit < SUFFIXES.length - 1) {
			rounded = Math.round(rounded / 100) / 10.0;
			unit++;
		}

		String number = rounded == Math.floor(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
		return (value < 0 ? "-" : "") + number + SUFFIXES[unit];
	}

	/** {@code 142519000} becomes {@code 1d 15h 35m 19s}. */
	public static String duration(long millis) {
		long totalSeconds = Math.max(0, millis) / 1000;
		long days = totalSeconds / 86400;
		long hours = totalSeconds % 86400 / 3600;
		long minutes = totalSeconds % 3600 / 60;
		long seconds = totalSeconds % 60;
		StringBuilder builder = new StringBuilder();

		if (days > 0) {
			builder.append(days).append("d ");
		}

		if (days > 0 || hours > 0) {
			builder.append(hours).append("h ");
		}

		if (days > 0 || hours > 0 || minutes > 0) {
			builder.append(minutes).append("m ");
		}

		return builder.append(seconds).append('s').toString();
	}
}
