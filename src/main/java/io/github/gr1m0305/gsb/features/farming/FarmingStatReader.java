package io.github.gr1m0305.gsb.features.farming;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gr1m0305.gsb.util.NumberFormats;
import org.jspecify.annotations.Nullable;

/**
 * What the tab list says about the {@link FarmingStat}s. Yaw and pitch are not in there, the tracker gets
 * those from the player.
 */
public final class FarmingStatReader {
	//  Speed: ✦400
	private static final Pattern WIDGET_LINE = Pattern.compile("^(?<name>[^:]+): (?<value>.+)$");

	private final Map<String, String> widgetValues = new HashMap<>();
	private final Optional<FarmingFortuneParser.Fortune> fortune;

	public FarmingStatReader(Collection<String> tabLines) {
		for (String line : tabLines) {
			Matcher matcher = WIDGET_LINE.matcher(line.trim());

			if (matcher.matches()) {
				widgetValues.putIfAbsent(matcher.group("name").trim(), matcher.group("value").trim());
			}
		}

		fortune = FarmingFortuneParser.parse(tabLines);
	}

	/** The crop whose fortune the tab list shows, or null when it shows none. */
	public @Nullable String crop() {
		return fortune.map(FarmingFortuneParser.Fortune::crop).orElse(null);
	}

	/** The name of a stat. The tab list says which crop the crop fortune is for, so the tracker does too. */
	public String label(FarmingStat stat) {
		String crop = crop();
		return stat == FarmingStat.CROP_FORTUNE && crop != null ? crop + " Fortune" : stat.toString();
	}

	/** The value of a stat, or nothing when the tab list does not have it. */
	public Optional<String> value(FarmingStat stat) {
		return switch (stat) {
			case TOTAL_FORTUNE -> fortune.map(value -> fortune(value.total()));
			case FARMING_FORTUNE -> fortune.map(value -> fortune(value.farmingFortune()));
			case CROP_FORTUNE -> fortune.filter(value -> value.crop() != null).map(value -> fortune(value.cropFortune()));
			case SPEED -> widgetValue("Speed");
			case BONUS_PEST_CHANCE -> widgetValue("Bonus Pest Chance");
			case FARMING_WISDOM -> widgetValue("Farming Wisdom");
			// The Pests widget only calls it "Cooldown".
			case PEST_COOLDOWN -> widgetValue("Cooldown");
			case YAW, PITCH -> Optional.empty();
		};
	}

	private Optional<String> widgetValue(String name) {
		return Optional.ofNullable(widgetValues.get(name));
	}

	private static String fortune(int value) {
		return "☘" + NumberFormats.grouped(value);
	}
}
