package io.github.gr1m0305.gsb.config;

import java.net.URI;
import java.util.List;

import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.notenoughupdates.moulconfig.Social;
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.minecraft.util.Util;

/** The GitHub button in the top right corner of the config menu. */
final class GitHubLink extends Social {
	private static final MyResourceLocation ICON = new MyResourceLocation(GrimsSkyblock.MOD_ID, "social/github.png");

	@Override
	public void onClick() {
		// MoulConfig's own link button goes through java.awt.Desktop, which Minecraft runs without.
		Util.getPlatform().openUri(URI.create(GrimsSkyblock.GITHUB_URL));
	}

	@Override
	public List<StructuredText> getTooltip() {
		return List.of(StructuredText.of("GitHub"));
	}

	@Override
	public MyResourceLocation getIcon() {
		return ICON;
	}
}
