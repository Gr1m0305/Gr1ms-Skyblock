package io.github.gr1m0305.gsb.features.farming;

import java.util.List;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.config.categories.FarmingCategory;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

/** Dings and shows big red text for a second when the pest cooldown on the Garden runs out. */
public final class PestCooldownAlert extends HudElement {
	private static final List<HudLine> TEXT = List.of(HudLine.of("§c§lPest Cooldown"));

	private final Supplier<FarmingCategory.PestCooldown> config;
	private final PestCooldownLogic logic = new PestCooldownLogic();
	private final AlertPlayback playback = new AlertPlayback();
	private @Nullable List<String> parsedTabLines;

	public PestCooldownAlert(Supplier<FarmingCategory.PestCooldown> config) {
		super("Pest Cooldown Alert", FarmingCategory.PestCooldown::defaultPosition);
		this.config = config;
	}

	public void tick(Minecraft minecraft) {
		if (!config.get().enabled || !SkyblockState.onGarden()) {
			logic.reset();
			parsedTabLines = null;
		} else {
			long now = System.currentTimeMillis();
			List<String> tabLines = SkyblockState.tabLines();
			boolean ranOut = false;

			// The tab list is read into a new list several times per second, only look at it when that happened.
			if (tabLines != parsedTabLines) {
				parsedTabLines = tabLines;
				ranOut = logic.tabListRead(PestCooldownParser.parse(tabLines), now);
			}

			if (logic.tick(now) || ranOut) {
				playback.start();
			}
		}

		if (playback.tick()) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARROW_HIT_PLAYER, 1f, 1f));
		}
	}

	/** Plays the alert once, to check how it looks and sounds. Closes the menu so the text can be seen. */
	public void test() {
		GameCompat.setScreen(Minecraft.getInstance(), null);
		playback.start();
	}

	/** The alert is nothing but its big red text. */
	@Override
	public @Nullable String title() {
		return null;
	}

	@Override
	public Position position() {
		return config.get().position;
	}

	@Override
	public boolean isEnabled() {
		return config.get().enabled;
	}

	@Override
	public boolean isVisible() {
		return playback.isActive();
	}

	@Override
	public List<HudLine> lines() {
		return playback.isActive() ? TEXT : List.of();
	}

	@Override
	public List<HudLine> previewLines() {
		return TEXT;
	}
}
