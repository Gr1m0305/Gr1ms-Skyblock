package io.github.gr1m0305.gsb.features.pets;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gr1m0305.gsb.util.NumberFormats;
import io.github.gr1m0305.gsb.util.StringUtil;
import org.jspecify.annotations.Nullable;

/** Reads the pet that is out from the Pet widget of the tab list. */
public final class PetWidgetParser {
	private static final String TITLE = "Pet:";
	private static final String NO_PET = "No pet selected";
	private static final String MAX_LEVEL = "MAX LEVEL";
	// [Lvl 100] Endermite, [Lvl 200] [108✦] Golden Dragon, and with a skin [Lvl 100] Black Cat ✦
	private static final Pattern PET = Pattern.compile("^\\[Lvl (?<level>\\d+)](?: \\[[^]]*])? (?<name>.+?)(?: ✦)?$");
	// 15,986.6/29k XP (53.6%)
	private static final Pattern EXPERIENCE = Pattern.compile("^[\\d,.]+/[\\d,.]+[kKmMbB]? XP \\((?<percent>[\\d.]+)%\\)$");
	// +21,248,020.7 XP, what a pet at its highest level has earned on top
	private static final Pattern OVERFLOW = Pattern.compile("^\\+(?<experience>[\\d,.]+) XP$");
	/**
	 * How far below its title the widget is searched. It has the pet, the item the pet holds and the
	 * experience, and where it runs into the next column of the tab list the title of that column is in between.
	 */
	private static final int LINES_SEARCHED = 5;

	/**
	 * @param fraction how much of the way to the next level is done, from 0 to 1
	 * @param text     that written out, for a pet at its highest level what the widget says instead
	 */
	public record Progress(float fraction, String text) {
	}

	/**
	 * @param progress null when the widget does not say how far the pet is
	 */
	public record Pet(String name, int level, @Nullable Progress progress) {
	}

	/**
	 * @param pet  the pet that is out, or null when none is
	 * @param line which line of the tab list the pet is on, its colour there is its rarity
	 */
	public record Widget(@Nullable Pet pet, int line) {
	}

	private PetWidgetParser() {
	}

	/**
	 * @param tabLines the tab list from top to bottom
	 * @return nothing when the tab list has no Pet widget
	 */
	public static Optional<Widget> parse(List<String> tabLines) {
		int title = tabLines.indexOf(TITLE);

		if (title < 0) {
			return Optional.empty();
		}

		int petLine = -1;
		String name = null;
		int level = 0;

		for (int i = title + 1; i < Math.min(tabLines.size(), title + 1 + LINES_SEARCHED); i++) {
			String line = tabLines.get(i);

			// The title of the next widget.
			if (line.endsWith(":")) {
				break;
			}

			if (name == null) {
				if (line.equals(NO_PET)) {
					return Optional.of(new Widget(null, i));
				}

				Matcher matcher = PET.matcher(line);

				if (matcher.matches()) {
					petLine = i;
					name = matcher.group("name").trim();
					level = Integer.parseInt(matcher.group("level"));
				}

				continue;
			}

			Progress progress = parseProgress(line);

			if (progress != null) {
				return Optional.of(new Widget(new Pet(name, level, progress), petLine));
			}
		}

		// A widget whose lines make no sense is as good as none.
		return name == null ? Optional.empty() : Optional.of(new Widget(new Pet(name, level, null), petLine));
	}

	private static @Nullable Progress parseProgress(String line) {
		if (line.equals(MAX_LEVEL)) {
			return new Progress(1f, MAX_LEVEL);
		}

		Matcher overflow = OVERFLOW.matcher(line);

		if (overflow.matches()) {
			return new Progress(1f, "+" + NumberFormats.compact(StringUtil.parseDouble(overflow.group("experience"))) + " XP");
		}

		Matcher experience = EXPERIENCE.matcher(line);

		if (experience.matches()) {
			float percent = (float) StringUtil.parseDouble(experience.group("percent"));
			return new Progress(Math.clamp(percent / 100f, 0f, 1f), experience.group("percent") + "%");
		}

		return null;
	}
}
