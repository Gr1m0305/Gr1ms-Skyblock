package io.github.gr1m0305.gsb.features.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.github.gr1m0305.gsb.util.StringUtil;
import org.junit.jupiter.api.Test;

class FarmingChatParserTest {
	@Test
	void readsNpcSales() {
		FarmingChatParser.NpcSale sale = FarmingChatParser.parseNpcSale("You sold Enchanted Melon Slice x64 for 20,480 Coins!").orElseThrow();
		assertEquals("Enchanted Melon Slice", sale.item());
		assertEquals(64, sale.count());
		assertEquals(20_480, sale.coins());
	}

	@Test
	void readsNpcSalesWithFormattingCodesStripped() {
		String raw = "§aYou sold §r§aEnchanted Sugar§r§8 x1,280§r§a for §r§6819,200 Coins§r§a!";
		FarmingChatParser.NpcSale sale = FarmingChatParser.parseNpcSale(StringUtil.stripFormatting(raw)).orElseThrow();
		assertEquals("Enchanted Sugar", sale.item());
		assertEquals(1_280, sale.count());
		assertEquals(819_200, sale.coins());
	}

	@Test
	void readsNpcSalesOfASingleItem() {
		FarmingChatParser.NpcSale single = FarmingChatParser.parseNpcSale("You sold Cropie x1 for 25,000 Coins!").orElseThrow();
		assertEquals(1, single.count());

		FarmingChatParser.NpcSale oneCoin = FarmingChatParser.parseNpcSale("You sold Rookie Hoe for 1 Coin!").orElseThrow();
		assertEquals("Rookie Hoe", oneCoin.item());
		assertEquals(1, oneCoin.count());
		assertEquals(1, oneCoin.coins());

		FarmingChatParser.NpcSale fraction = FarmingChatParser.parseNpcSale("You sold Seeds x3 for 1.5 Coins!").orElseThrow();
		assertEquals(1.5, fraction.coins());
	}

	@Test
	void ignoresOtherMessagesAboutSelling() {
		assertTrue(FarmingChatParser.parseNpcSale("[Bazaar] Sold 64x Enchanted Melon Slice for 19,692 coins!").isEmpty());
		assertTrue(FarmingChatParser.parseNpcSale("Steve: You sold Wheat x1 for 6 Coins! lol").isEmpty());
	}

	@Test
	void readsPestLoot() {
		FarmingChatParser.PestLoot loot = FarmingChatParser.parsePestLoot("You received 7x Enchanted Potato for killing a Locust!").orElseThrow();
		assertEquals("Enchanted Potato", loot.item());
		assertEquals(7, loot.amount());
		assertEquals("Locust", loot.pest());

		assertEquals("Earthworm", FarmingChatParser.parsePestLoot("You received 64x Enchanted Melon Slice for killing an Earthworm!").orElseThrow().pest());
		assertTrue(FarmingChatParser.parsePestLoot("You received 5 Copper for killing a Locust!").isEmpty());
	}

	@Test
	void readsSackWithdrawals() {
		FarmingChatParser.SackWithdrawal withdrawal = FarmingChatParser.parseSackWithdrawal("Moved 3 Enchanted Hay Bale from your Sacks to your inventory.").orElseThrow();
		assertEquals("Enchanted Hay Bale", withdrawal.item());
		assertEquals(3, withdrawal.amount());
	}

	@Test
	void recognisesSackMessages() {
		assertTrue(FarmingChatParser.isSackMessage(StringUtil.stripFormatting("§6[Sacks] §r§a+1,234§r§e items§r§e.§r§8 (Last 5s.)")));
		assertFalse(FarmingChatParser.isSackMessage("Steve: [Sacks] +5 items."));
	}

	@Test
	void readsHowFarBackASackMessageGoes() {
		assertEquals(5, FarmingChatParser.sackMessageSeconds("[Sacks] +1,246 items, -160 items. (Last 5s.)"));
		assertEquals(30, FarmingChatParser.sackMessageSeconds("[Sacks] -2 items. (Last 30s.)"));
		// Better to look back too far than not far enough.
		assertEquals(10, FarmingChatParser.sackMessageSeconds("[Sacks] +5 items."));
	}

	@Test
	void readsTheHoverTextOfSackMessages() {
		String hover = """
				Added items:
				  +1,234 Wheat (Agronomy Sack)
				  +12 Enchanted Melon Slice (Enchanted Agronomy Sack)
				  +3 Seeds (Agronomy Sack, Enchanted Agronomy Sack)

				This message can be disabled in the settings.""";

		assertEquals(List.of(
				new FarmingChatParser.SackChange("Wheat", 1_234),
				new FarmingChatParser.SackChange("Enchanted Melon Slice", 12),
				new FarmingChatParser.SackChange("Seeds", 3)), FarmingChatParser.parseSackHover(hover));
	}

	@Test
	void readsRemovedItemsAsNegative() {
		String hover = "Removed items:\n  -2,000 Enchanted Potato (Enchanted Agronomy Sack)\n  + 2 other items";
		assertEquals(List.of(new FarmingChatParser.SackChange("Enchanted Potato", -2_000)), FarmingChatParser.parseSackHover(hover));
	}
}
