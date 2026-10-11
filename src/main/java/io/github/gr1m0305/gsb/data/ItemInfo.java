package io.github.gr1m0305.gsb.data;

/**
 * What the Hypixel API knows about a SkyBlock item.
 *
 * @param npcSellPrice coins an NPC pays for one, or 0 when it cannot be sold to NPCs
 */
public record ItemInfo(String id, String name, String tier, double npcSellPrice) {
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
