package io.github.gr1m0305.gsb.features.farming;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gr1m0305.gsb.util.StringUtil;
import org.jspecify.annotations.Nullable;

/** Reads the fortune stats out of the Stats widget of the tab list. */
public final class FarmingFortuneParser {
	//  Farming Fortune: ☘1724
	private static final Pattern FORTUNE_LINE = Pattern.compile("^(?<name>.+?) Fortune: \\D*(?<value>\\d[\\d,]*(?:\\.\\d+)?)$");
	// Fortune stats of other skills, everything else that ends in "Fortune" is a crop.
	private static final Set<String> NOT_CROPS = Set.of("mining", "foraging", "fishing", "combat", "hunter", "ore", "block",
			"dwarven metal", "gemstone", "fig", "mangrove");

	/**
	 * @param crop the crop the crop fortune belongs to, or null when the tab list shows none
	 */
	public record Fortune(int farmingFortune, int cropFortune, @Nullable String crop) {
		public int total() {
			return farmingFortune + cropFortune;
		}
	}

	private FarmingFortuneParser() {
	}

	/** Returns nothing when the tab list has no Farming Fortune line. */
	public static Optional<Fortune> parse(Collection<String> tabLines) {
		Integer farmingFortune = null;
		int cropFortune = 0;
		String crop = null;

		for (String line : tabLines) {
			Matcher matcher = FORTUNE_LINE.matcher(line.trim());

			if (!matcher.matches()) {
				continue;
			}

			String name = matcher.group("name").trim();
			int value = (int) Math.round(StringUtil.parseDouble(matcher.group("value")));

			if (name.equalsIgnoreCase("Farming")) {
				farmingFortune = value;
			} else if (crop == null && !NOT_CROPS.contains(name.toLowerCase(Locale.ROOT))) {
				crop = name;
				cropFortune = value;
			}
		}

		if (farmingFortune == null) {
			return Optional.empty();
		}

		return Optional.of(new Fortune(farmingFortune, cropFortune, crop));
	}
}
