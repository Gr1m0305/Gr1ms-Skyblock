package io.github.gr1m0305.gsb.config.categories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.gr1m0305.gsb.features.farming.FarmingStat;
import org.junit.jupiter.api.Test;

class FarmingCategoryTest {
	// Set up like the one that reads the config file.
	private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	private static FarmingCategory load(String json) {
		FarmingCategory farming = GSON.fromJson(json, FarmingCategory.class);
		farming.sanitize();
		return farming;
	}

	@Test
	void theStatTrackerStartsWithTheDefaultStats() {
		FarmingCategory farming = load("{}");
		assertEquals(FarmingStat.DEFAULTS, farming.statTracker.stats);
		assertFalse(farming.statTracker.enabled);
	}

	@Test
	void keepsTheSettingsOfTheFarmingFortuneDisplay() {
		FarmingCategory farming = load("""
				{"fortuneDisplay":{"enabled":true,"position":{"x":300,"y":40,"scale":1.5,"centered":false}}}""");

		assertTrue(farming.statTracker.enabled);
		assertEquals(300, farming.statTracker.position.x);
		assertEquals(40, farming.statTracker.position.y);
		assertEquals(1.5f, farming.statTracker.position.scale);
		assertEquals(FarmingStat.DEFAULTS, farming.statTracker.stats);
	}

	@Test
	void savesAndLoadsThePickedStats() {
		FarmingCategory farming = new FarmingCategory();
		farming.statTracker.stats.clear();
		farming.statTracker.stats.addAll(List.of(FarmingStat.PITCH, FarmingStat.CROP_FORTUNE));

		String json = GSON.toJson(farming);
		assertTrue(json.contains("\"statTracker\""), json);
		assertFalse(json.contains("fortuneDisplay"), json);
		assertEquals(List.of(FarmingStat.PITCH, FarmingStat.CROP_FORTUNE), load(json).statTracker.stats);
	}

	@Test
	void thePestCooldownAlertLastsTwoSecondsUnlessChanged() {
		assertEquals(2f, load("{}").pestCooldown.alertSeconds);
		assertEquals(4.5f, load("{\"pestCooldown\":{\"alertSeconds\":4.5}}").pestCooldown.alertSeconds);
		// What a config file edited by hand can hold.
		assertEquals(0.5f, load("{\"pestCooldown\":{\"alertSeconds\":0}}").pestCooldown.alertSeconds);
		assertEquals(10f, load("{\"pestCooldown\":{\"alertSeconds\":3600}}").pestCooldown.alertSeconds);
	}

	@Test
	void repairsAStatListEditedByHand() {
		FarmingCategory farming = load("""
				{"statTracker":{"stats":["SPEED","NOT_A_STAT","YAW","SPEED"]}}""");
		assertEquals(List.of(FarmingStat.SPEED, FarmingStat.YAW), farming.statTracker.stats);

		// The config menu adds to this list.
		farming.statTracker.stats.add(FarmingStat.PITCH);

		assertEquals(FarmingStat.DEFAULTS, load("{\"statTracker\":{\"stats\":null}}").statTracker.stats);
		// Picking nothing at all is allowed.
		assertTrue(load("{\"statTracker\":{\"stats\":[]}}").statTracker.stats.isEmpty());
	}
}
