package io.github.gr1m0305.gsb.skyblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.gr1m0305.gsb.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

/** Where on SkyBlock the player is, read from the tab list four times per second. */
public final class SkyblockState {
	private static final int SCAN_INTERVAL_TICKS = 5;
	private static final String AREA_PREFIX = "Area: ";
	/**
	 * The order the game shows the entries of the tab list in. Hypixel writes its widgets line by line into
	 * entries that sort this way, so this is also the order of those lines.
	 */
	private static final Comparator<PlayerInfo> TAB_LIST_ORDER = Comparator.<PlayerInfo>comparingInt(player -> -player.getTabListOrder())
			.thenComparingInt(player -> player.getGameMode() == GameType.SPECTATOR ? 1 : 0)
			.thenComparing(SkyblockState::teamName)
			.thenComparing(player -> player.getProfile().name(), String::compareToIgnoreCase);

	private static List<String> tabLines = List.of();
	private static List<Component> tabComponents = List.of();
	private static boolean onSkyblock;
	private static boolean onGarden;
	private static int ticksUntilScan;

	private SkyblockState() {
	}

	/**
	 * The text of all tab list entries from top to bottom, without formatting codes. A new list object every
	 * time the tab list is read.
	 */
	public static List<String> tabLines() {
		return tabLines;
	}

	/** The entries of {@link #tabLines()} as the server sent them, for when their colours matter. */
	public static List<Component> tabComponents() {
		return tabComponents;
	}

	/** Whether the player is on SkyBlock at all, on whichever island. */
	public static boolean onSkyblock() {
		return onSkyblock;
	}

	public static boolean onGarden() {
		return onGarden;
	}

	public static void tick(Minecraft minecraft) {
		if (ticksUntilScan-- > 0) {
			return;
		}

		ticksUntilScan = SCAN_INTERVAL_TICKS - 1;
		ClientPacketListener connection = minecraft.getConnection();

		if (connection == null || minecraft.level == null) {
			tabLines = List.of();
			tabComponents = List.of();
			onSkyblock = false;
			onGarden = false;
			return;
		}

		List<PlayerInfo> players = new ArrayList<>(connection.getListedOnlinePlayers());
		players.sort(TAB_LIST_ORDER);
		List<String> lines = new ArrayList<>();
		List<Component> components = new ArrayList<>();
		String area = null;

		for (PlayerInfo player : players) {
			Component displayName = player.getTabListDisplayName();

			if (displayName == null) {
				continue;
			}

			String line = StringUtil.stripFormatting(displayName.getString()).trim();

			if (line.isEmpty()) {
				continue;
			}

			lines.add(line);
			components.add(displayName);

			if (area == null && line.startsWith(AREA_PREFIX)) {
				area = line.substring(AREA_PREFIX.length()).trim();
			}
		}

		tabLines = List.copyOf(lines);
		tabComponents = List.copyOf(components);
		Scoreboard scoreboard = minecraft.level.getScoreboard();
		// Only SkyBlock has an Area widget. Without it in the tab list the scoreboard still says where the player is.
		onSkyblock = area != null || scoreboardShowsSkyblock(scoreboard);
		onGarden = area != null ? area.equalsIgnoreCase("Garden") : scoreboardShowsGarden(scoreboard);
	}

	private static String teamName(PlayerInfo player) {
		PlayerTeam team = player.getTeam();
		return team != null ? team.getName() : "";
	}

	/** The title of the scoreboard is SKYBLOCK on every island, with CO-OP or GUEST behind it at times. */
	private static boolean scoreboardShowsSkyblock(@Nullable Scoreboard scoreboard) {
		if (scoreboard == null) {
			return false;
		}

		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		return sidebar != null && StringUtil.stripFormatting(sidebar.getDisplayName().getString()).toUpperCase(Locale.ROOT).contains("SKYBLOCK");
	}

	private static boolean scoreboardShowsGarden(@Nullable Scoreboard scoreboard) {
		if (scoreboard == null) {
			return false;
		}

		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

		if (sidebar == null) {
			return false;
		}

		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String line = StringUtil.stripFormatting(PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString());

			// "⏣ The Garden" in the barn, "⏣ Plot - 3" on the plots.
			if (line.contains("⏣") && (line.contains("The Garden") || line.contains("Plot"))) {
				return true;
			}
		}

		return false;
	}
}
