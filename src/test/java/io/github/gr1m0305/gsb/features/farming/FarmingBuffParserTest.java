package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import io.github.gr1m0305.gsb.features.farming.FarmingBuffParser.Buffs;
import io.github.gr1m0305.gsb.features.farming.FarmingBuffParser.PestFortune;
import io.github.gr1m0305.gsb.features.farming.FarmingBuffParser.Spray;
import org.junit.jupiter.api.Test;

class FarmingBuffParserTest {
	/** The Pests widget with the two lines in question, between the widgets it sits between. */
	private static Buffs parse(String spray, String bonus) {
		return FarmingBuffParser.parse(List.of("Area: Garden", "Pests:", "Alive: 2", "Plots: 4, 12", spray, bonus, "Cooldown: 1m 58s", "Visitors: (3)", "Stats:"));
	}

	@Test
	void readsHowLongThePestFortuneLasts() {
		assertEquals(Optional.of(new PestFortune(200, "29m")), parse("Spray: None", "Bonus: +200☘ 29m").pestFortune());
		assertEquals(Optional.of(new PestFortune(200, "5m 2s")), parse("Spray: None", "Bonus: +200☘ 5m 2s").pestFortune());
		assertEquals(Optional.of(new PestFortune(60, "8s")), parse("Spray: None", "Bonus: +60☘ 8s").pestFortune());
		// However the fortune is written.
		assertEquals(Optional.of(new PestFortune(1_200, "29m")), parse("Spray: None", "Bonus: +1,200 29m").pestFortune());
		assertTrue(parse("Spray: None", "Bonus: +200☘ 29m").pestFortune().orElseThrow().isActive());
	}

	@Test
	void knowsWhenThePestFortuneIsOver() {
		PestFortune pestFortune = parse("Spray: None", "Bonus: INACTIVE").pestFortune().orElseThrow();
		assertEquals(PestFortune.INACTIVE, pestFortune);
		assertFalse(pestFortune.isActive());
	}

	@Test
	void readsTheSprayOfThePlot() {
		assertEquals(Optional.of(new Spray("Compost", "12m")), parse("Spray: Compost (12m)", "Bonus: INACTIVE").spray());
		assertEquals(Optional.of(new Spray("Compost", "1m 3s")), parse("Spray: Compost (1m 3s)", "Bonus: INACTIVE").spray());
		assertEquals(Optional.of(new Spray("Honey Jar", "53s")), parse("Spray: Honey Jar (53s)", "Bonus: INACTIVE").spray());
		assertTrue(parse("Spray: Compost (12m)", "Bonus: INACTIVE").spray().orElseThrow().isActive());
	}

	@Test
	void knowsWhenThePlotIsNotSprayed() {
		Spray spray = parse("Spray: None", "Bonus: INACTIVE").spray().orElseThrow();
		assertEquals(Spray.NONE, spray);
		assertFalse(spray.isActive());
	}

	@Test
	void hasNothingWithoutThePestsWidget() {
		assertEquals(Buffs.UNKNOWN, FarmingBuffParser.parse(List.of()));
		assertEquals(Buffs.UNKNOWN, FarmingBuffParser.parse(List.of("Area: Garden", "Stats:", "Speed: ✦400")));
		// The lines of some other widget are not the buffs.
		assertEquals(Buffs.UNKNOWN, FarmingBuffParser.parse(List.of("Area: Garden", "Bonus: +200☘ 29m", "Spray: Compost (12m)")));
	}

	@Test
	void onlyLooksAtThePestsWidget() {
		Buffs buffs = FarmingBuffParser.parse(List.of("Pests:", "Alive: 0", "Cooldown: READY", "Stats:", "Bonus: +200☘ 29m", "Spray: Compost (12m)"));
		assertEquals(Buffs.UNKNOWN, buffs);
	}

	@Test
	void skipsTheTitleOfTheNextColumn() {
		// A widget can start at the bottom of one column of the tab list and go on below the title of the next.
		Buffs buffs = FarmingBuffParser.parse(List.of("Pests:", "Alive: 0", "Plots: 3", "Info", "Spray: Dung (30m)", "Bonus: +100☘ 14m 59s", "Cooldown: READY"));
		assertEquals(new Buffs(Optional.of(new PestFortune(100, "14m 59s")), Optional.of(new Spray("Dung", "30m"))), buffs);
	}

	@Test
	void aWidgetCanHaveOnlyOneOfTheTwo() {
		Buffs buffs = FarmingBuffParser.parse(List.of("Pests:", "Alive: 0", "Spray: None", "Cooldown: READY"));
		assertEquals(new Buffs(Optional.empty(), Optional.of(Spray.NONE)), buffs);
	}
}
