package io.github.gr1m0305.gsb.data;

import static java.util.Map.entry;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * The Hypixel API says what an item looks like with the names Minecraft 1.8 had for items. This knows what
 * those are called today, for the things that farming on the Garden gets you.
 */
public final class LegacyMaterials {
	/**
	 * Only the names that changed. In 1.8 one material could be several items that were told apart by a
	 * number, those are listed as {@code MATERIAL:number}.
	 */
	private static final Map<String, String> RENAMED = Map.ofEntries(
			entry("CARROT_ITEM", "carrot"),
			entry("CARROT_STICK", "carrot_on_a_stick"),
			entry("POTATO_ITEM", "potato"),
			entry("MELON", "melon_slice"),
			entry("MELON_BLOCK", "melon"),
			entry("SPECKLED_MELON", "glistering_melon_slice"),
			entry("NETHER_STALK", "nether_wart"),
			entry("SEEDS", "wheat_seeds"),
			entry("HUGE_MUSHROOM_1", "brown_mushroom_block"),
			entry("HUGE_MUSHROOM_2", "red_mushroom_block"),
			entry("MUSHROOM_SOUP", "mushroom_stew"),
			entry("YELLOW_FLOWER", "dandelion"),
			entry("WATER_LILY", "lily_pad"),

			entry("INK_SACK:0", "ink_sac"),
			entry("INK_SACK:1", "red_dye"),
			entry("INK_SACK:2", "green_dye"),
			entry("INK_SACK:3", "cocoa_beans"),
			entry("INK_SACK:4", "lapis_lazuli"),
			entry("INK_SACK:5", "purple_dye"),
			entry("INK_SACK:6", "cyan_dye"),
			entry("INK_SACK:7", "light_gray_dye"),
			entry("INK_SACK:8", "gray_dye"),
			entry("INK_SACK:9", "pink_dye"),
			entry("INK_SACK:10", "lime_dye"),
			entry("INK_SACK:11", "yellow_dye"),
			entry("INK_SACK:12", "light_blue_dye"),
			entry("INK_SACK:13", "magenta_dye"),
			entry("INK_SACK:14", "orange_dye"),
			entry("INK_SACK:15", "bone_meal"),

			entry("DOUBLE_PLANT:0", "sunflower"),
			entry("DOUBLE_PLANT:1", "lilac"),
			entry("DOUBLE_PLANT:2", "tall_grass"),
			entry("DOUBLE_PLANT:3", "large_fern"),
			entry("DOUBLE_PLANT:4", "rose_bush"),
			entry("DOUBLE_PLANT:5", "peony"),

			entry("RED_ROSE:0", "poppy"),
			entry("RED_ROSE:1", "blue_orchid"),
			entry("RED_ROSE:2", "allium"),
			entry("RED_ROSE:3", "azure_bluet"),
			entry("RED_ROSE:4", "red_tulip"),
			entry("RED_ROSE:5", "orange_tulip"),
			entry("RED_ROSE:6", "white_tulip"),
			entry("RED_ROSE:7", "pink_tulip"),
			entry("RED_ROSE:8", "oxeye_daisy"),

			entry("SKULL_ITEM:0", "skeleton_skull"),
			entry("SKULL_ITEM:1", "wither_skeleton_skull"),
			entry("SKULL_ITEM:2", "zombie_head"),
			entry("SKULL_ITEM:3", "player_head"),
			entry("SKULL_ITEM:4", "creeper_head"),

			// The vinyls that pests drop.
			entry("GOLD_RECORD", "music_disc_13"),
			entry("GREEN_RECORD", "music_disc_cat"),
			entry("RECORD_3", "music_disc_blocks"),
			entry("RECORD_4", "music_disc_chirp"),
			entry("RECORD_5", "music_disc_far"),
			entry("RECORD_6", "music_disc_mall"),
			entry("RECORD_7", "music_disc_mellohi"),
			entry("RECORD_8", "music_disc_stal"),
			entry("RECORD_9", "music_disc_strad"),
			entry("RECORD_10", "music_disc_ward"),
			entry("RECORD_11", "music_disc_11"),
			entry("RECORD_12", "music_disc_wait"),

			entry("GOLD_HOE", "golden_hoe"),
			entry("GOLD_AXE", "golden_axe"),
			entry("WOOD_HOE", "wooden_hoe"),
			entry("WOOD_AXE", "wooden_axe"));

	private LegacyMaterials() {
	}

	/**
	 * The name of the item in the {@code minecraft} namespace. A material this does not know comes back in
	 * lower case, which is right for everything that was never renamed.
	 */
	public static String modernName(String material, int durability) {
		String renamed = RENAMED.get(material + ":" + durability);

		if (renamed == null) {
			renamed = RENAMED.get(material);
		}

		return renamed != null ? renamed : material.toLowerCase(Locale.ROOT);
	}

	/** Every name this can turn a material into by itself, for checking them against the game. */
	public static Collection<String> modernNames() {
		return RENAMED.values();
	}
}
