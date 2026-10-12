package io.github.gr1m0305.gsb.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

class PetTexturesTest {
	// A trimmed copy of items/ELEPHANT;4.json from the NotEnoughUpdates item list.
	private static final String ELEPHANT = """
			{
			  "itemid": "minecraft:skull",
			  "displayname": "§7[Lvl {LVL}] §6Elephant",
			  "nbttag": "{ExtraAttributes:{id:\\"ELEPHANT;4\\",petInfo:\\"{\\\\\\"type\\\\\\":\\\\\\"ELEPHANT\\\\\\",\\\\\\"tier\\\\\\":\\\\\\"LEGENDARY\\\\\\"}\\"},HideFlags:254,SkullOwner:{Id:\\"759e6cd3-e045-33cd-b12d-d2bdb1431706\\",Properties:{textures:[0:{Name:\\"textures\\",Signature:\\"XlRgdfyqzn2IJEkz+/0q=\\",Value:\\"ewogICJ0aW1lc3RhbXAiIDogMTcxOTU5+/A=\\"}]}},display:{Name:\\"§7[Lvl {LVL}] §6Elephant\\"}}",
			  "internalname": "ELEPHANT;4"
			}""";

	@Test
	void findsTheTextureInAnItemFile() {
		assertEquals("ewogICJ0aW1lc3RhbXAiIDogMTcxOTU5+/A=", PetTextures.parseTexture(new StringReader(ELEPHANT)));
	}

	@Test
	void itemsThatAreNoHeadsHaveNoTexture() {
		assertNull(PetTextures.parseTexture(new StringReader("{\"itemid\":\"minecraft:bone\",\"nbttag\":\"{HideFlags:254}\"}")));
		assertNull(PetTextures.parseTexture(new StringReader("{\"itemid\":\"minecraft:bone\"}")));
	}

	@Test
	void namesPetsLikeTheItemList() {
		assertEquals("ELEPHANT", PetTextures.petType("Elephant"));
		assertEquals("MOOSHROOM_COW", PetTextures.petType("Mooshroom Cow"));
		assertEquals("GOLDEN_DRAGON", PetTextures.petType(" Golden Dragon "));
		assertEquals("T_REX", PetTextures.petType("T-Rex"));
		assertEquals("", PetTextures.petType("✦"));
	}

	@Test
	void triesTheRarityOfThePetFirst() {
		assertEquals(List.of(1, 4, 3, 2, 0, 5), PetTextures.raritiesToTry(1));
		assertEquals(List.of(4, 3, 2, 1, 0, 5), PetTextures.raritiesToTry(4));
		assertEquals(List.of(5, 4, 3, 2, 1, 0), PetTextures.raritiesToTry(5));
		assertEquals(List.of(4, 3, 2, 1, 0, 5), PetTextures.raritiesToTry(PetTextures.UNKNOWN_RARITY));
		assertEquals(List.of(4, 3, 2, 1, 0, 5), PetTextures.raritiesToTry(9));
	}

	@Test
	void readsTheRarityFromTheColourOfTheName() {
		assertEquals(0, PetTextures.rarityOfColor(0xFFFFFF));
		assertEquals(1, PetTextures.rarityOfColor(0x55FF55));
		assertEquals(2, PetTextures.rarityOfColor(0x5555FF));
		assertEquals(3, PetTextures.rarityOfColor(0xAA00AA));
		assertEquals(4, PetTextures.rarityOfColor(0xFFAA00));
		assertEquals(5, PetTextures.rarityOfColor(0xFF55FF));
		// With the alpha the game adds to colours it draws.
		assertEquals(4, PetTextures.rarityOfColor(0xFFFFAA00));
		assertEquals(PetTextures.UNKNOWN_RARITY, PetTextures.rarityOfColor(0x123456));
	}
}
