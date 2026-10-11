package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FarmingFortuneParserTest {
	@Test
	void addsFarmingAndCropFortune() {
		FarmingFortuneParser.Fortune fortune = FarmingFortuneParser.parse(List.of(
				"Area: Garden",
				"Stats:",
				"Speed: ✦400",
				"Farming Fortune: ☘1203",
				"Wheat Fortune: ☘521",
				"Pests:")).orElseThrow();

		assertEquals(1203, fortune.farmingFortune());
		assertEquals(521, fortune.cropFortune());
		assertEquals("Wheat", fortune.crop());
		assertEquals(1724, fortune.total());
	}

	@Test
	void worksWithoutACropFortuneLine() {
		FarmingFortuneParser.Fortune fortune = FarmingFortuneParser.parse(List.of(" Farming Fortune: ☘1,203")).orElseThrow();
		assertEquals(1203, fortune.total());
		assertNull(fortune.crop());
	}

	@Test
	void cropNamesCanHaveSeveralWords() {
		FarmingFortuneParser.Fortune fortune = FarmingFortuneParser.parse(List.of("Farming Fortune: ☘1000", "Cocoa Beans Fortune: ☘250.4")).orElseThrow();
		assertEquals("Cocoa Beans", fortune.crop());
		assertEquals(1250, fortune.total());
	}

	@Test
	void fortuneOfOtherSkillsIsNotACrop() {
		FarmingFortuneParser.Fortune fortune = FarmingFortuneParser.parse(List.of(
				"Mining Fortune: ☘300",
				"Foraging Fortune: ☘80",
				"Farming Fortune: ☘900",
				"Sugar Cane Fortune: ☘100")).orElseThrow();

		assertEquals("Sugar Cane", fortune.crop());
		assertEquals(1000, fortune.total());
	}

	@Test
	void nothingWithoutAFarmingFortuneLine() {
		assertTrue(FarmingFortuneParser.parse(List.of("Area: Garden", "Wheat Fortune: ☘521")).isEmpty());
		assertTrue(FarmingFortuneParser.parse(List.of()).isEmpty());
	}
}
