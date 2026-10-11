package io.github.gr1m0305.gsb.skyblock;

import java.util.ArrayList;
import java.util.List;

import io.github.gr1m0305.gsb.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

/** Where on SkyBlock the player is, read from the tab list a couple of times per second. */
public final class SkyblockState {
	private static final int SCAN_INTERVAL_TICKS = 10;
	private static final String AREA_PREFIX = "Area: ";

	private static List<String> tabLines = List.of();
	private static boolean onGarden;
	private static int ticksUntilScan;

	private SkyblockState() {
	}

	/** The text of all tab list entries, without formatting codes. A new list object every time the tab list is read. */
	public static List<String> tabLines() {
		return tabLines;
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
			onGarden = false;
			return;
		}

		List<String> lines = new ArrayList<>();
		String area = null;

		for (PlayerInfo player : connection.getListedOnlinePlayers()) {
			Component displayName = player.getTabListDisplayName();

			if (displayName == null) {
				continue;
			}

			String line = StringUtil.stripFormatting(displayName.getString()).trim();

			if (line.isEmpty()) {
				continue;
			}

			lines.add(line);

			if (area == null && line.startsWith(AREA_PREFIX)) {
				area = line.substring(AREA_PREFIX.length()).trim();
			}
		}

		tabLines = List.copyOf(lines);
		// Without the Area widget in the tab list the scoreboard still names the location.
		onGarden = area != null ? area.equalsIgnoreCase("Garden") : scoreboardShowsGarden(minecraft.level.getScoreboard());
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
