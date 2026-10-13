package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class FarmingStatReaderTest {
	private static final FarmingStatReader GARDEN = new FarmingStatReader(List.of(
			"Area: Garden",
			"Pests:",
			" Alive: 0",
			" Cooldown: 1m 58s",
			"Stats:",
			" Speed: ✦400",
			" Farming Fortune: ☘1203",
			" Wheat Fortune: ☘521",
			" Bonus Pest Chance: ൠ190%",
			" Overbloom: 25",
			" Farming Wisdom: ☯48.5"));

	@Test
	void readsTheStatsOfTheTabList() {
		assertEquals(Optional.of("☘1,724"), GARDEN.value(FarmingStat.TOTAL_FORTUNE));
		assertEquals(Optional.of("☘1,203"), GARDEN.value(FarmingStat.FARMING_FORTUNE));
		assertEquals(Optional.of("☘521"), GARDEN.value(FarmingStat.CROP_FORTUNE));
		assertEquals(Optional.of("✦400"), GARDEN.value(FarmingStat.SPEED));
		assertEquals(Optional.of("ൠ190%"), GARDEN.value(FarmingStat.BONUS_PEST_CHANCE));
		// The symbol of Overbloom is a character of Hypixel's resource pack.
		assertEquals(Optional.of("25"), GARDEN.value(FarmingStat.OVERBLOOM));
		assertEquals(Optional.of("☯48.5"), GARDEN.value(FarmingStat.FARMING_WISDOM));
		assertEquals(Optional.of("1m 58s"), GARDEN.value(FarmingStat.PEST_COOLDOWN));
	}

	@Test
	void namesTheCropOfTheCropFortune() {
		assertEquals("Wheat", GARDEN.crop());
		assertEquals("Wheat Fortune", GARDEN.label(FarmingStat.CROP_FORTUNE));
		assertEquals("Total Fortune", GARDEN.label(FarmingStat.TOTAL_FORTUNE));
		assertEquals("Pest Cooldown", GARDEN.label(FarmingStat.PEST_COOLDOWN));
	}

	@Test
	void hasNothingForStatsTheTabListDoesNotShow() {
		FarmingStatReader reader = new FarmingStatReader(List.of("Area: Garden", "Stats:", " Farming Fortune: ☘900", "Pests:", " Cooldown: READY"));

		// Without a crop the total is the Farming Fortune alone.
		assertEquals(Optional.of("☘900"), reader.value(FarmingStat.TOTAL_FORTUNE));
		assertTrue(reader.value(FarmingStat.CROP_FORTUNE).isEmpty());
		assertNull(reader.crop());
		assertEquals("Crop Fortune", reader.label(FarmingStat.CROP_FORTUNE));
		assertTrue(reader.value(FarmingStat.SPEED).isEmpty());
		assertTrue(reader.value(FarmingStat.BONUS_PEST_CHANCE).isEmpty());
		assertTrue(reader.value(FarmingStat.OVERBLOOM).isEmpty());
		assertEquals(Optional.of("READY"), reader.value(FarmingStat.PEST_COOLDOWN));

		for (FarmingStat stat : FarmingStat.values()) {
			assertTrue(new FarmingStatReader(List.of()).value(stat).isEmpty(), stat + " without a tab list");
		}
	}

	@Test
	void yawAndPitchAreNotInTheTabList() {
		assertTrue(GARDEN.value(FarmingStat.YAW).isEmpty());
		assertTrue(GARDEN.value(FarmingStat.PITCH).isEmpty());
	}

	@Test
	void onlyLinesWithAValueCount() {
		// The titles of widgets, like "Stats:", have nothing behind the colon. Of two lines for a stat the first is kept.
		FarmingStatReader reader = new FarmingStatReader(List.of("Speed:", "Speed: ✦350", "Speed: ✦1"));
		assertEquals(Optional.of("✦350"), reader.value(FarmingStat.SPEED));
	}
}
