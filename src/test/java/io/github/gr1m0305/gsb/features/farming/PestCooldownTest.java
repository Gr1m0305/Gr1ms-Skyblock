package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.github.gr1m0305.gsb.features.farming.PestCooldownParser.Cooldown;
import org.junit.jupiter.api.Test;

class PestCooldownTest {
	private final PestCooldownLogic logic = new PestCooldownLogic();

	private static Cooldown parse(String cooldownLine) {
		return PestCooldownParser.parse(List.of("Area: Garden", "Pests:", "Alive: 0", cooldownLine, "Stats:"));
	}

	@Test
	void readsTheCooldownFromTheTabList() {
		assertEquals(Cooldown.counting(118), parse("Cooldown: 1m 58s"));
		assertEquals(Cooldown.counting(58), parse("Cooldown: 58s"));
		assertEquals(Cooldown.counting(120), parse("Cooldown: 2m"));
		assertEquals(Cooldown.counting(1), parse(" Cooldown: 1s"));
		assertEquals(Cooldown.READY, parse("Cooldown: READY"));
		assertEquals(Cooldown.READY, parse("Cooldown: 0s"));
	}

	@Test
	void anythingElseIsUnknown() {
		assertEquals(Cooldown.UNKNOWN, parse("Cooldown: MAX PESTS"));
		assertEquals(Cooldown.UNKNOWN, parse("Cooldown:"));
		assertEquals(Cooldown.UNKNOWN, parse("Bonus: INACTIVE"));
		assertEquals(Cooldown.UNKNOWN, PestCooldownParser.parse(List.of()));
	}

	@Test
	void alertsWhenTheTabListTurnsReady() {
		assertFalse(logic.tabListRead(Cooldown.counting(118), 0));
		assertFalse(logic.tick(1_000));
		assertFalse(logic.tabListRead(Cooldown.counting(60), 58_000));
		assertFalse(logic.tick(58_000));
		assertTrue(logic.tabListRead(Cooldown.READY, 118_000));
		// Only once.
		assertFalse(logic.tabListRead(Cooldown.READY, 118_250));
		assertFalse(logic.tick(118_250));
	}

	@Test
	void doesNotAlertForACooldownThatWasAlreadyOver() {
		assertFalse(logic.tabListRead(Cooldown.READY, 0));
		assertFalse(logic.tick(50));
		assertFalse(logic.tabListRead(Cooldown.UNKNOWN, 250));
		assertFalse(logic.tabListRead(Cooldown.READY, 500));
	}

	@Test
	void countsTheLastSecondsDownItself() {
		assertFalse(logic.tabListRead(Cooldown.counting(3), 10_000));
		// The tab list is not refreshed again, the three seconds still pass.
		assertFalse(logic.tick(12_950));
		assertTrue(logic.tick(13_000));
		assertFalse(logic.tick(13_050));
		// The tab list catching up afterwards is not a second alert.
		assertFalse(logic.tabListRead(Cooldown.counting(1), 13_100));
		assertFalse(logic.tick(14_200));
		assertFalse(logic.tabListRead(Cooldown.READY, 14_250));
	}

	@Test
	void everyNewNumberOnTheTabListCorrectsTheCountdown() {
		assertFalse(logic.tabListRead(Cooldown.counting(5), 10_000));
		// The 5 had already been showing for a while when it was first read, the 4 comes early.
		assertFalse(logic.tabListRead(Cooldown.counting(4), 10_300));
		assertFalse(logic.tabListRead(Cooldown.counting(4), 10_550));
		assertFalse(logic.tick(14_250));
		assertTrue(logic.tick(14_300));
	}

	@Test
	void doesNotCountDownLongCooldownsItself() {
		assertFalse(logic.tabListRead(Cooldown.counting(60), 0));
		// Should the timer stand still on the server, a minute passing here means nothing.
		assertFalse(logic.tick(61_000));
		assertTrue(logic.tabListRead(Cooldown.READY, 300_000));
	}

	@Test
	void alertsAgainForTheNextCooldown() {
		assertFalse(logic.tabListRead(Cooldown.counting(2), 0));
		assertTrue(logic.tick(2_000));
		// A pest spawned right away, the tab list never showed READY in between.
		assertFalse(logic.tabListRead(Cooldown.counting(135), 2_500));
		assertFalse(logic.tabListRead(Cooldown.counting(1), 136_500));
		assertTrue(logic.tick(137_500));
	}

	@Test
	void maxPestsHidesTheCooldownWithoutLosingIt() {
		assertFalse(logic.tabListRead(Cooldown.counting(8), 0));
		assertFalse(logic.tabListRead(Cooldown.UNKNOWN, 1_000));
		// Nothing to count down while the tab list shows something else.
		assertFalse(logic.tick(9_000));
		assertTrue(logic.tabListRead(Cooldown.READY, 20_000));
	}

	@Test
	void resetForgetsARunningCooldown() {
		assertFalse(logic.tabListRead(Cooldown.counting(3), 0));
		logic.reset();
		assertFalse(logic.tick(3_000));
		assertFalse(logic.tabListRead(Cooldown.READY, 3_500));
	}

	@Test
	void theAlertDingsForAsLongAsItLasts() {
		AlertPlayback playback = new AlertPlayback();
		assertFalse(playback.isActive());
		assertFalse(playback.tick());

		// 20 ticks are one second.
		assertEquals(List.of(0, 4, 8, 12, 16), dingTicks(playback, 1f, 20));
		assertEquals(List.of(0, 4, 8, 12, 16, 20, 24, 28, 32, 36), dingTicks(playback, 2f, 40));
		assertEquals(List.of(0, 4, 8), dingTicks(playback, 0.5f, 10));
		// Nothing the slider's text box takes can make an alert that never shows or never ends.
		assertEquals(List.of(0), dingTicks(playback, 0f, 1));
		assertEquals(List.of(0), dingTicks(playback, -3f, 1));
		assertEquals(List.of(0), dingTicks(playback, Float.NaN, 1));
	}

	/** Plays an alert to its end and returns the ticks it dinged in. */
	private static List<Integer> dingTicks(AlertPlayback playback, float seconds, int expectedTicks) {
		playback.start(seconds);
		List<Integer> dings = new ArrayList<>();
		int activeTicks = 0;

		for (int tick = 0; tick < 400; tick++) {
			if (playback.isActive()) {
				activeTicks++;
			}

			if (playback.tick()) {
				dings.add(tick);
			}
		}

		assertEquals(expectedTicks, activeTicks);
		assertFalse(playback.isActive());
		return dings;
	}
}
