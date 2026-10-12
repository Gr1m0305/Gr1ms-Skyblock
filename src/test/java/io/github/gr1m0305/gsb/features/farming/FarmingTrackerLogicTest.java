package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FarmingTrackerLogicTest {
	private static final Map<String, Double> PRICES = Map.of("WHEAT", 6.0, "ENCHANTED_WHEAT", 960.0, "ENCHANTED_MELON", 320.0, "CROPIE", 25_000.0);

	private final FarmingTrackerLogic logic = new FarmingTrackerLogic(new FarmingTrackerData());

	private FarmingTrackerLogic.Summary summary(int maxRows) {
		return logic.summarize(id -> PRICES.getOrDefault(id, 0.0), maxRows);
	}

	private long count(String id) {
		return logic.data().items.getOrDefault(id, 0L);
	}

	@Test
	void countsInventoryAndSackGains() {
		logic.inventoryChanged("ENCHANTED_MELON", 5, 1_000);
		logic.sackChanged("WHEAT", 1_234, 2_000);
		logic.sackChanged("WHEAT", 100, 7_000);

		assertEquals(5, count("ENCHANTED_MELON"));
		assertEquals(1_334, count("WHEAT"));
		assertEquals(5 * 320 + 1_334 * 6, summary(10).totalProfit());
	}

	@Test
	void compactingKeepsTheValueOfTheRawItemsOutOfTheTotal() {
		logic.inventoryChanged("WHEAT", 160, 1_000);
		// The Personal Compactor swaps 160 Wheat for one enchanted item.
		logic.inventoryChanged("WHEAT", -160, 1_050);
		logic.inventoryChanged("ENCHANTED_WHEAT", 1, 1_050);

		assertEquals(0, count("WHEAT"));
		assertEquals(1, count("ENCHANTED_WHEAT"));
		assertEquals(960, summary(10).totalProfit());
	}

	@Test
	void itemsUsedUpLowerTheProfitButAreNotListed() {
		logic.inventoryChanged("ENCHANTED_MELON", 10, 1_000);
		logic.inventoryChanged("CROPIE", -1, 2_000);

		FarmingTrackerLogic.Summary summary = summary(10);
		assertEquals(1, summary.rows().size());
		assertEquals("ENCHANTED_MELON", summary.rows().getFirst().id());
		assertEquals(10 * 320 - 25_000, summary.totalProfit());
	}

	@Test
	void sackLossesAreIgnored() {
		logic.sackChanged("WHEAT", 500, 1_000);
		logic.sackChanged("WHEAT", -400, 2_000);
		assertEquals(500, count("WHEAT"));
	}

	@Test
	void itemsPutIntoASackByHandAreNotCountedTwice() {
		logic.inventoryChanged("ENCHANTED_MELON", 64, 1_000);
		logic.insertedIntoSack("ENCHANTED_MELON", 64, 20_000);
		// The sack message reports the 64 moved items together with 10 that were farmed meanwhile.
		logic.sackChanged("ENCHANTED_MELON", 74, 24_000);

		assertEquals(74, count("ENCHANTED_MELON"));
	}

	@Test
	void sackInsertsAreForgottenAfterAWhile() {
		logic.insertedIntoSack("WHEAT", 64, 1_000);
		logic.sackChanged("WHEAT", 50, 1_000 + FarmingTrackerLogic.SACK_INSERT_LIFETIME_MILLIS + 1);
		assertEquals(50, count("WHEAT"));
	}

	@Test
	void takingItemsOutOfTheSacksIsNotAGainWhenChatComesFirst() {
		logic.withdrawnFromSack("ENCHANTED_MELON", 32, 1_000);
		logic.inventoryChanged("ENCHANTED_MELON", 32, 1_050);
		assertEquals(0, count("ENCHANTED_MELON"));

		// Later gains of the same item count again.
		logic.inventoryChanged("ENCHANTED_MELON", 3, 60_000);
		assertEquals(3, count("ENCHANTED_MELON"));
	}

	@Test
	void takingItemsOutOfTheSacksIsNotAGainWhenTheInventoryComesFirst() {
		logic.inventoryChanged("ENCHANTED_MELON", 7, 500);
		logic.inventoryChanged("ENCHANTED_MELON", 32, 1_000);
		logic.withdrawnFromSack("ENCHANTED_MELON", 32, 1_050);
		assertEquals(7, count("ENCHANTED_MELON"));
	}

	@Test
	void anOldGainIsNotMistakenForASackWithdrawal() {
		logic.inventoryChanged("ENCHANTED_MELON", 32, 1_000);
		long later = 1_000 + FarmingTrackerLogic.SACK_WITHDRAWAL_WINDOW_MILLIS + 1_000;
		logic.withdrawnFromSack("ENCHANTED_MELON", 32, later);
		logic.inventoryChanged("ENCHANTED_MELON", 32, later + 50);
		assertEquals(32, count("ENCHANTED_MELON"));
	}

	@Test
	void listsTheMostValuableItemsFirstAndSumsUpTheRest() {
		logic.sackChanged("WHEAT", 1_000, 1_000);
		logic.inventoryChanged("ENCHANTED_MELON", 100, 1_000);
		logic.inventoryChanged("CROPIE", 2, 1_000);
		logic.inventoryChanged("MYSTERY_ITEM", 9, 1_000);

		FarmingTrackerLogic.Summary summary = summary(2);
		assertEquals("CROPIE", summary.rows().get(0).id());
		assertEquals("ENCHANTED_MELON", summary.rows().get(1).id());
		assertEquals(2, summary.hiddenItems());
		assertEquals(6_000, summary.hiddenValue());
		assertEquals(50_000 + 32_000 + 6_000, summary.totalProfit());
	}

	@Test
	void uptimeOnlyRunsWhileFarming() {
		logic.tick(1_000, 50, true);
		logic.tick(1_050, 50, false);
		assertEquals(100, logic.data().uptimeMillis);

		// Long after the last sign of farming the clock stands still.
		long idle = 1_050 + FarmingTrackerLogic.IDLE_TIMEOUT_MILLIS + 1;
		logic.tick(idle, 50, false);
		assertEquals(100, logic.data().uptimeMillis);

		// A gain counts as farming too.
		logic.sackChanged("WHEAT", 10, idle + 50);
		logic.tick(idle + 100, 50, false);
		assertEquals(150, logic.data().uptimeMillis);
	}

	@Test
	void pausesAfterAFewSecondsWithoutFarming() {
		// Nothing was farmed yet.
		assertTrue(logic.isPaused(1_000));

		logic.tick(1_000, 50, true);
		assertFalse(logic.isPaused(1_000));
		assertFalse(logic.isPaused(1_000 + FarmingTrackerLogic.IDLE_TIMEOUT_MILLIS));
		assertTrue(logic.isPaused(1_001 + FarmingTrackerLogic.IDLE_TIMEOUT_MILLIS));
		assertTrue(FarmingTrackerLogic.IDLE_TIMEOUT_MILLIS <= 5_000, "a few seconds, not more");

		// Killing a pest, an item coming in and holding the mouse button all end the pause.
		logic.pestKilled(20_000);
		assertFalse(logic.isPaused(20_000));
		assertTrue(logic.isPaused(40_000));
		logic.inventoryChanged("CROPIE", 1, 40_000);
		assertFalse(logic.isPaused(40_000));
		assertTrue(logic.isPaused(60_000));
		logic.tick(60_000, 50, true);
		assertFalse(logic.isPaused(60_000));

		// Items leaving the inventory and selling to an NPC are no farming.
		logic.inventoryChanged("CROPIE", -1, 80_000);
		logic.soldToNpc(100, 1);
		assertTrue(logic.isPaused(80_000));
	}

	@Test
	void profitPerHourUsesTheUptime() {
		logic.inventoryChanged("CROPIE", 4, 1_000);
		logic.data().uptimeMillis = 30 * 60 * 1000;
		assertEquals(200_000, summary(10).profitPerHour());
	}

	@Test
	void tracksPestsAndNpcSales() {
		logic.pestKilled(1_000);
		logic.pestKilled(2_000);
		logic.soldToNpc(20_480, 64);
		logic.soldToNpc(1.5, 3);

		FarmingTrackerLogic.Summary summary = summary(10);
		assertEquals(2, summary.pestsKilled());
		assertEquals(20_481.5, summary.npcCoins());
		assertEquals(67, summary.npcItemsSold());
		// Coins from selling are shown on their own, the items sold were already counted when they were farmed.
		assertEquals(0, summary.totalProfit());
	}

	@Test
	void resetClearsEverything() {
		logic.inventoryChanged("CROPIE", 4, 1_000);
		logic.pestKilled(1_000);
		logic.soldToNpc(100, 1);
		logic.tick(1_000, 50, true);
		assertTrue(logic.takeDirty());

		logic.reset();
		FarmingTrackerLogic.Summary summary = summary(10);
		assertTrue(summary.rows().isEmpty());
		assertEquals(0, summary.pestsKilled());
		assertEquals(0, summary.npcCoins());
		assertEquals(0, summary.uptimeMillis());
		assertTrue(logic.takeDirty());
		assertFalse(logic.takeDirty());
	}
}
