package io.github.gr1m0305.gsb.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HypixelDataTest {
	// Trimmed copies of what the Hypixel API returns.
	private static final String ITEMS = """
			{"success":true,"lastUpdated":1791302445914,"items":[
			{"material":"MELON","name":"Melon Slice","npc_sell_price":2,"id":"MELON"},
			{"material":"MELON","name":"Enchanted Melon Slice","tier":"UNCOMMON","npc_sell_price":320,"id":"ENCHANTED_MELON"},
			{"material":"HAY_BLOCK","name":"Enchanted Hay Bale","tier":"UNCOMMON","id":"ENCHANTED_HAY_BLOCK"},
			{"material":"PAPER","name":"Overclocker 3000","tier":"LEGENDARY","npc_sell_price":250000,"id":"OVERCLOCKER_3000"},
			{"material":"INK_SACK","durability":3,"name":"Cocoa Beans","npc_sell_price":3,"id":"INK_SACK:3"},
			{"material":"RED_ROSE","name":"§cRose Bouquet","tier":"RARE","id":"ROSE_BOUQUET"},
			{"material":"CAULDRON","name":"Cauldron","id":"CAULDRON"},
			{"material":"CAULDRON_ITEM","name":"Cauldron","npc_sell_price":5,"id":"CAULDRON_ITEM"}
			]}""";

	private static final String BAZAAR = """
			{"success":true,"products":{
			"ENCHANTED_MELON":{"product_id":"ENCHANTED_MELON",
			"sell_summary":[{"amount":611443,"pricePerUnit":307.7,"orders":9},{"amount":94377,"pricePerUnit":307.6,"orders":2}],
			"buy_summary":[{"amount":10985,"pricePerUnit":324.1,"orders":8}],
			"quick_status":{"productId":"ENCHANTED_MELON","sellPrice":306.9,"buyPrice":324.86}},
			"ENCHANTED_HAY_BLOCK":{"product_id":"ENCHANTED_HAY_BLOCK","sell_summary":[],"buy_summary":[],
			"quick_status":{"productId":"ENCHANTED_HAY_BLOCK","sellPrice":0.0,"buyPrice":0.0}}
			}}""";

	private static ItemRepository repository() {
		ItemRepository repository = new ItemRepository(Path.of("unused"));
		repository.setItems(ItemRepository.parseApiResponse(new StringReader(ITEMS)));
		return repository;
	}

	@Test
	void readsItems() {
		ItemRepository repository = repository();

		ItemInfo melon = repository.get("ENCHANTED_MELON");
		assertEquals("Enchanted Melon Slice", melon.name());
		assertEquals(320, melon.npcSellPrice());
		assertEquals('a', melon.rarityColor());

		assertEquals(0, repository.get("ENCHANTED_HAY_BLOCK").npcSellPrice());
		assertEquals('f', repository.get("MELON").rarityColor());
		assertEquals('6', repository.get("OVERCLOCKER_3000").rarityColor());
		assertEquals("Rose Bouquet", repository.get("ROSE_BOUQUET").name());
		assertNull(repository.get("NOT_AN_ITEM"));
	}

	@Test
	void findsIdsByTheNameInChat() {
		ItemRepository repository = repository();
		assertEquals("ENCHANTED_MELON", repository.idFromName("Enchanted Melon Slice"));
		assertEquals("INK_SACK:3", repository.idFromName("§fCocoa Beans"));
		assertEquals("ENCHANTED_HAY_BLOCK", repository.idFromName("enchanted hay bale"));
		// Of two items with the same name the one an NPC buys is the one players farm and sell.
		assertEquals("CAULDRON_ITEM", repository.idFromName("Cauldron"));
		assertEquals("SOME_NEW_CROP", repository.idFromName("Some New Crop"));
	}

	@Test
	void namesItemsItDoesNotKnow() {
		ItemRepository repository = repository();
		assertEquals("Overclocker 3000", repository.displayName("OVERCLOCKER_3000"));
		assertEquals("Some New Crop", repository.displayName("SOME_NEW_CROP"));
		assertEquals("Ink Sack 4", repository.displayName("INK_SACK:4"));
	}

	@Test
	void survivesTheCacheFile() {
		Map<String, ItemInfo> items = ItemRepository.parseApiResponse(new StringReader(ITEMS));
		String cached = ItemRepository.toCacheJson(items).toString();
		assertEquals(items, ItemRepository.parseCache(new StringReader(cached)));
	}

	@Test
	void readsBazaarPrices() {
		Map<String, BazaarPrices.Price> prices = BazaarPrices.parseApiResponse(new StringReader(BAZAAR));
		assertEquals(new BazaarPrices.Price(307.7, 324.1), prices.get("ENCHANTED_MELON"));
		assertEquals(new BazaarPrices.Price(0, 0), prices.get("ENCHANTED_HAY_BLOCK"));
	}

	@Test
	void priceSourcesFallBackToEachOther() {
		ItemInfo melon = repository().get("ENCHANTED_MELON");
		ItemInfo hayBale = repository().get("ENCHANTED_HAY_BLOCK");
		BazaarPrices.Price melonBazaar = new BazaarPrices.Price(307.7, 324.1);
		BazaarPrices.Price hayBazaar = new BazaarPrices.Price(150, 170);

		assertEquals(320, PriceSource.NPC_SELL.price(melon, melonBazaar));
		assertEquals(307.7, PriceSource.BAZAAR_INSTANT_SELL.price(melon, melonBazaar));
		assertEquals(324.1, PriceSource.BAZAAR_SELL_OFFER.price(melon, melonBazaar));
		assertEquals(320, PriceSource.HIGHEST.price(melon, melonBazaar));

		// Nothing an NPC pays for, so the Bazaar price it is.
		assertEquals(150, PriceSource.NPC_SELL.price(hayBale, hayBazaar));
		// Not on the Bazaar, so the NPC price it is.
		assertEquals(320, PriceSource.BAZAAR_INSTANT_SELL.price(melon, null));
		assertEquals(0, PriceSource.HIGHEST.price(null, null));
	}
}
