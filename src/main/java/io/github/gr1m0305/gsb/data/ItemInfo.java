package io.github.gr1m0305.gsb.data;

/**
 * What the Hypixel API knows about a SkyBlock item.
 *
 * @param npcSellPrice coins an NPC pays for one, or 0 when it cannot be sold to NPCs
 */
public record ItemInfo(String id, String name, String tier, double npcSellPrice, Appearance appearance) {
	/**
	 * What an item looks like. The API still describes that the way Minecraft 1.8 did, see {@link LegacyMaterials}.
	 *
	 * @param material   the 1.8 name of the item it is drawn as, empty when unknown
	 * @param durability which of the variants of that material it is, cocoa beans are number 3 of INK_SACK
	 * @param glowing    whether it shines like an enchanted item
	 * @param skin       the texture of an item that is a player head, empty for all others
	 * @param model      the model Hypixel's resource pack draws it with, empty when it has none
	 */
	public record Appearance(String material, int durability, boolean glowing, String skin, String model) {
		public static final Appearance UNKNOWN = new Appearance("", 0, false, "", "");
	}

	/** The formatting code of the item's rarity colour. */
	public char rarityColor() {
		return switch (tier) {
			case "UNCOMMON" -> 'a';
			case "RARE" -> '9';
			case "EPIC" -> '5';
			case "LEGENDARY" -> '6';
			case "MYTHIC" -> 'd';
			case "DIVINE" -> 'b';
			case "SPECIAL", "VERY_SPECIAL" -> 'c';
			case "SUPREME", "ULTIMATE", "UNOBTAINABLE" -> '4';
			default -> 'f';
		};
	}
}
