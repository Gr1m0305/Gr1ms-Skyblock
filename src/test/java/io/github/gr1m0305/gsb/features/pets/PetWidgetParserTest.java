package io.github.gr1m0305.gsb.features.pets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.github.gr1m0305.gsb.features.pets.PetWidgetParser.Pet;
import io.github.gr1m0305.gsb.features.pets.PetWidgetParser.Progress;
import io.github.gr1m0305.gsb.features.pets.PetWidgetParser.Widget;
import org.junit.jupiter.api.Test;

class PetWidgetParserTest {
	private static Widget parse(String... widgetLines) {
		List<String> tabLines = new ArrayList<>(List.of("Area: Garden", "Server: mini12B", "Pet:"));
		tabLines.addAll(List.of(widgetLines));
		tabLines.addAll(List.of("Stats:", "Speed: ✦400"));
		return PetWidgetParser.parse(tabLines).orElseThrow();
	}

	@Test
	void readsThePetAndItsProgress() {
		Widget widget = parse("[Lvl 87] Mooshroom Cow", "Yellow Bandana", "153,251.5/250k XP (61.3%)");

		assertEquals(new Pet("Mooshroom Cow", 87, new Progress(61.3f / 100f, "61.3%")), widget.pet());
		// The pet is on the fourth line of that tab list.
		assertEquals(3, widget.line());
	}

	@Test
	void worksWithoutAPetItem() {
		assertEquals(new Pet("Rabbit", 1, new Progress(0f, "0%")), parse("[Lvl 1] Rabbit", "0/100 XP (0%)").pet());
		assertEquals(new Pet("Elephant", 99, new Progress(99.9f / 100f, "99.9%")), parse("[Lvl 99] Elephant", "1,884,999.9/1.9M XP (99.9%)").pet());
	}

	@Test
	void aPetAtItsHighestLevelHasAFullBar() {
		assertEquals(new Pet("Endermite", 100, new Progress(1f, "MAX LEVEL")), parse("[Lvl 100] Endermite", "MAX LEVEL").pet());
		// Some pets keep counting what they earn after that.
		assertEquals(new Pet("Elephant", 100, new Progress(1f, "+21.2M XP")), parse("[Lvl 100] Elephant", "Washed-up Souvenir", "+21,248,020.7 XP").pet());
	}

	@Test
	void leavesOutWhatIsNotPartOfTheName() {
		// The Golden Dragon shows a second number, and a skin puts a star behind the name.
		assertEquals("Golden Dragon", parse("[Lvl 200] [108✦] Golden Dragon", "MAX LEVEL").pet().name());
		assertEquals(200, parse("[Lvl 200] [108✦] Golden Dragon", "MAX LEVEL").pet().level());
		assertEquals("Black Cat", parse("[Lvl 100] Black Cat ✦", "MAX LEVEL").pet().name());
	}

	@Test
	void knowsWhenNoPetIsOut() {
		Widget widget = parse("No pet selected");
		assertNull(widget.pet());
	}

	@Test
	void skipsTheTitleOfTheNextColumn() {
		// A widget can start at the bottom of one column of the tab list and go on below the title of the next.
		assertEquals(new Pet("Bee", 42, new Progress(0.5f, "50%")), parse("Info", "[Lvl 42] Bee", "5/10 XP (50%)").pet());
		assertEquals(new Pet("Bee", 42, new Progress(0.5f, "50%")), parse("[Lvl 42] Bee", "Info", "Yellow Bandana", "5/10 XP (50%)").pet());
	}

	@Test
	void doesNotReadIntoTheNextWidget() {
		List<String> tabLines = List.of("Pet:", "[Lvl 42] Bee", "Skills:", "5/10 XP (50%)");
		assertEquals(new Pet("Bee", 42, null), PetWidgetParser.parse(tabLines).orElseThrow().pet());
	}

	@Test
	void nothingWithoutThePetWidget() {
		assertTrue(PetWidgetParser.parse(List.of("Area: Garden", "Stats:", "Pet Luck: ♣12", "[Lvl 42] Bee")).isEmpty());
		assertTrue(PetWidgetParser.parse(List.of()).isEmpty());
		// A title with nothing this understands below it.
		assertTrue(PetWidgetParser.parse(List.of("Pet:", "Stats:", "Speed: ✦400")).isEmpty());
		assertTrue(PetWidgetParser.parse(List.of("Pet:")).isEmpty());
	}
}
