package io.github.gr1m0305.gsb.features.farming;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gr1m0305.gsb.util.StringUtil;

/**
 * Reads the buffs that run out from the Pests widget of the tab list: the Farming Fortune bonus the
 * Pesthunter gives for pests, and the spray on the plot the player is in.
 */
public final class FarmingBuffParser {
	private static final String WIDGET_TITLE = "Pests:";
	/** The widget has about six lines. Where it runs into the next column of the tab list, the title of that column is in between. */
	private static final int WIDGET_LINES = 8;
	// Bonus: INACTIVE, Bonus: +200☘ 29m, Bonus: +200☘ 5m 2s, Bonus: +200☘ 8s
	private static final Pattern BONUS = Pattern.compile("^Bonus: (?:(?<inactive>INACTIVE)|\\+(?<fortune>[\\d,]+)\\D* (?<time>\\d.*))$");
	// Spray: None, Spray: Compost (12m), Spray: Honey Jar (1m 3s), Spray: Compost (53s)
	private static final Pattern SPRAY = Pattern.compile("^Spray: (?<spray>[^()]+?)(?: \\((?<time>[^()]+)\\))?$");

	/**
	 * @param timeLeft how long it still lasts the way the tab list writes it, like {@code 29m} or {@code 5m 2s}
	 */
	public record PestFortune(int fortune, String timeLeft) {
		/** The buff ran out, or pests were never traded in for it. */
		public static final PestFortune INACTIVE = new PestFortune(0, "");

		public boolean isActive() {
			return !timeLeft.isEmpty();
		}
	}

	/**
	 * @param name     what the plot was sprayed with, like {@code Compost}
	 * @param timeLeft how long it still lasts the way the tab list writes it, like {@code 12m} or {@code 53s}
	 */
	public record Spray(String name, String timeLeft) {
		/** The plot is not sprayed. */
		public static final Spray NONE = new Spray("", "");

		public boolean isActive() {
			return !timeLeft.isEmpty();
		}
	}

	/**
	 * Each of the two is empty when the tab list does not have its line.
	 *
	 * @param spray the spray on the plot the player is in, which is the only one the tab list shows
	 */
	public record Buffs(Optional<PestFortune> pestFortune, Optional<Spray> spray) {
		public static final Buffs UNKNOWN = new Buffs(Optional.empty(), Optional.empty());
	}

	private FarmingBuffParser() {
	}

	/**
	 * @param tabLines the tab list from top to bottom
	 */
	public static Buffs parse(List<String> tabLines) {
		int title = tabLines.indexOf(WIDGET_TITLE);

		if (title < 0) {
			return Buffs.UNKNOWN;
		}

		Optional<PestFortune> pestFortune = Optional.empty();
		Optional<Spray> spray = Optional.empty();

		for (int i = title + 1; i < Math.min(tabLines.size(), title + 1 + WIDGET_LINES); i++) {
			String line = tabLines.get(i);

			// The title of the next widget.
			if (line.endsWith(":")) {
				break;
			}

			Matcher bonus = BONUS.matcher(line);

			if (bonus.matches() && pestFortune.isEmpty()) {
				pestFortune = Optional.of(bonus.group("inactive") != null ? PestFortune.INACTIVE
						: new PestFortune((int) StringUtil.parseLong(bonus.group("fortune")), bonus.group("time").trim()));
				continue;
			}

			Matcher sprayed = SPRAY.matcher(line);

			if (sprayed.matches() && spray.isEmpty()) {
				String time = sprayed.group("time");
				// Only "Spray: None" comes without a time.
				spray = Optional.of(time == null ? Spray.NONE : new Spray(sprayed.group("spray").trim(), time.trim()));
			}
		}

		return new Buffs(pestFortune, spray);
	}
}
