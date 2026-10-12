package io.github.gr1m0305.gsb.features.farming;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gr1m0305.gsb.util.StringUtil;

/** Recognises the Hypixel chat messages the Farming Profit Tracker cares about. All input is text without formatting codes. */
public final class FarmingChatParser {
	// You sold Enchanted Melon Slice x64 for 20,480 Coins!
	private static final Pattern NPC_SALE = Pattern.compile("^You sold (?<item>.+?)(?: x(?<count>[\\d,]+))? for (?<coins>[\\d,]+(?:\\.\\d+)?) Coins?!$");
	// You received 7x Enchanted Potato for killing a Locust!
	private static final Pattern PEST_LOOT = Pattern.compile("^You received (?<amount>[\\d,]+)x (?<item>.+) for killing an? (?<pest>.+)!$");
	// Moved 3 Enchanted Hay Bale from your Sacks to your inventory.
	private static final Pattern SACK_WITHDRAWAL = Pattern.compile("^Moved (?<amount>[\\d,]+) (?<item>.+) from your Sacks to your inventory\\.$");
	// One line of the hover text of a [Sacks] message:   +1,234 Wheat (Agronomy Sack)
	private static final Pattern SACK_CHANGE = Pattern.compile("^\\s*(?<delta>[+-][\\d,]+) (?<item>.+?) \\([^()]+\\)\\s*$");
	// The end of a [Sacks] message: (Last 5s.)
	private static final Pattern SACK_PERIOD = Pattern.compile("\\(Last (?<seconds>\\d{1,4})s\\.\\)");
	/** For a [Sacks] message that does not say how far back it goes. */
	private static final int DEFAULT_SACK_PERIOD_SECONDS = 10;

	public record NpcSale(String item, long count, double coins) {
	}

	public record PestLoot(String item, long amount, String pest) {
	}

	public record SackWithdrawal(String item, long amount) {
	}

	public record SackChange(String item, long delta) {
	}

	private FarmingChatParser() {
	}

	public static Optional<NpcSale> parseNpcSale(String message) {
		Matcher matcher = NPC_SALE.matcher(message);

		if (!matcher.matches()) {
			return Optional.empty();
		}

		long count = matcher.group("count") != null ? StringUtil.parseLong(matcher.group("count")) : 1;
		return Optional.of(new NpcSale(matcher.group("item"), count, StringUtil.parseDouble(matcher.group("coins"))));
	}

	public static Optional<PestLoot> parsePestLoot(String message) {
		Matcher matcher = PEST_LOOT.matcher(message);

		if (!matcher.matches()) {
			return Optional.empty();
		}

		return Optional.of(new PestLoot(matcher.group("item"), StringUtil.parseLong(matcher.group("amount")), matcher.group("pest")));
	}

	public static Optional<SackWithdrawal> parseSackWithdrawal(String message) {
		Matcher matcher = SACK_WITHDRAWAL.matcher(message);

		if (!matcher.matches()) {
			return Optional.empty();
		}

		return Optional.of(new SackWithdrawal(matcher.group("item"), StringUtil.parseLong(matcher.group("amount"))));
	}

	public static boolean isSackMessage(String message) {
		return message.startsWith("[Sacks]");
	}

	/** How many seconds back the changes of a [Sacks] message go, which it says at its end. */
	public static int sackMessageSeconds(String message) {
		Matcher matcher = SACK_PERIOD.matcher(message);
		return matcher.find() ? Integer.parseInt(matcher.group("seconds")) : DEFAULT_SACK_PERIOD_SECONDS;
	}

	/** Reads the item list Hypixel shows when hovering over a [Sacks] message. */
	public static List<SackChange> parseSackHover(String hoverText) {
		List<SackChange> changes = new ArrayList<>();

		for (String line : hoverText.split("\n")) {
			Matcher matcher = SACK_CHANGE.matcher(line);

			if (matcher.matches()) {
				changes.add(new SackChange(matcher.group("item"), StringUtil.parseLong(matcher.group("delta").replace("+", ""))));
			}
		}

		return changes;
	}
}
