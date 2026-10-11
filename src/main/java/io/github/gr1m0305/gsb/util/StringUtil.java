package io.github.gr1m0305.gsb.util;

import java.util.regex.Pattern;

public final class StringUtil {
	private static final Pattern FORMATTING_CODE = Pattern.compile("§.");

	private StringUtil() {
	}

	/** Removes legacy formatting codes like {@code §a}. */
	public static String stripFormatting(String text) {
		if (text.indexOf('§') < 0) {
			return text;
		}

		return FORMATTING_CODE.matcher(text).replaceAll("");
	}

	/** Parses numbers as Hypixel writes them, e.g. {@code 1,234}. */
	public static long parseLong(String number) {
		return Long.parseLong(number.replace(",", ""));
	}

	/** Parses numbers as Hypixel writes them, e.g. {@code 1,234.5}. */
	public static double parseDouble(String number) {
		return Double.parseDouble(number.replace(",", ""));
	}
}
