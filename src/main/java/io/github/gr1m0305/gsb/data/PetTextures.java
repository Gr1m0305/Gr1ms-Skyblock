package io.github.gr1m0305.gsb.data;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import org.jspecify.annotations.Nullable;

/**
 * The textures of the heads that pets are. The Hypixel API does not have them, so they come from the item
 * list the NotEnoughUpdates mod keeps on GitHub, which has one small file per pet and rarity. Every texture
 * is only downloaded once and then kept on disk.
 */
public final class PetTextures {
	/** For a pet whose rarity is not known. */
	public static final int UNKNOWN_RARITY = -1;

	private static final String ITEMS_URL = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/";
	/** The rarities from common to mythic, numbered the way the item list numbers them. */
	private static final int RARITIES = 6;
	// Inside the item's NBT: Properties:{textures:[0:{Name:"textures",Signature:"...",Value:"ewogICJ0aW1l..."}]}
	private static final Pattern TEXTURE = Pattern.compile("Value:\"(?<value>[A-Za-z0-9+/=]+)\"");
	private static final long RETRY_INTERVAL_MILLIS = 10 * 60 * 1000L;

	private final Path cacheFile;
	private final Map<String, String> textures = new ConcurrentHashMap<>();
	private final Map<String, Long> lastAttempts = new ConcurrentHashMap<>();

	public PetTextures(Path cacheFile) {
		this.cacheFile = cacheFile;
	}

	/** Loads the textures downloaded earlier. */
	public void init() {
		if (!Files.exists(cacheFile)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(cacheFile)) {
			for (Map.Entry<String, JsonElement> entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
				textures.put(entry.getKey(), entry.getValue().getAsString());
			}
		} catch (IOException | RuntimeException e) {
			GrimsSkyblock.LOGGER.warn("Could not read the pet texture cache", e);
		}
	}

	/**
	 * The texture of a pet's head. A pet that is asked for the first time is downloaded in the background.
	 *
	 * @param petName the name of the pet as the game shows it, like {@code Mooshroom Cow}
	 * @param rarity  0 for common up to 5 for mythic, or {@link #UNKNOWN_RARITY}
	 * @return null as long as the texture is not there, which it stays for pets the item list does not have
	 */
	public @Nullable String get(String petName, int rarity) {
		String type = petType(petName);
		String key = type + ";" + rarity;
		String texture = textures.get(key);

		if (texture == null && !type.isEmpty()) {
			long now = System.currentTimeMillis();
			Long lastAttempt = lastAttempts.get(key);

			if (lastAttempt == null || now - lastAttempt >= RETRY_INTERVAL_MILLIS) {
				lastAttempts.put(key, now);
				HttpFetcher.EXECUTOR.execute(() -> download(key, type, rarity));
			}
		}

		return texture;
	}

	private void download(String key, String type, int rarity) {
		try {
			for (int candidate : raritiesToTry(rarity)) {
				try {
					String texture = HttpFetcher.fetch(ITEMS_URL + type + "%3B" + candidate + ".json", PetTextures::parseTexture);

					if (texture != null) {
						textures.put(key, texture);
						save();
						return;
					}
				} catch (HttpFetcher.StatusException e) {
					// Not every pet comes in every rarity, the next one has the same head.
					if (e.status != 404) {
						throw e;
					}
				}
			}

			GrimsSkyblock.LOGGER.info("There is no texture for the pet {}", type);
		} catch (IOException | RuntimeException e) {
			GrimsSkyblock.LOGGER.warn("Could not download the texture of the pet {}", type, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void save() throws IOException {
		JsonObject object = new JsonObject();
		textures.forEach(object::addProperty);
		Files.createDirectories(cacheFile.getParent());

		try (Writer writer = Files.newBufferedWriter(cacheFile)) {
			writer.write(object.toString());
		}
	}

	/**
	 * The rarity that a colour stands for in SkyBlock, which is how the tab list says how rare a pet is.
	 *
	 * @param rgb the colour of the pet's name
	 */
	public static int rarityOfColor(int rgb) {
		return switch (rgb & 0xFFFFFF) {
			case 0xFFFFFF -> 0;
			case 0x55FF55 -> 1;
			case 0x5555FF -> 2;
			case 0xAA00AA -> 3;
			case 0xFFAA00 -> 4;
			case 0xFF55FF -> 5;
			default -> UNKNOWN_RARITY;
		};
	}

	/** The name the item list files a pet under: {@code Mooshroom Cow} is {@code MOOSHROOM_COW}. */
	static String petType(String petName) {
		return petName.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
	}

	/** The rarity asked for comes first. A pet looks the same in every rarity, so any other one will do after that. */
	static List<Integer> raritiesToTry(int rarity) {
		List<Integer> rarities = new ArrayList<>();

		if (rarity >= 0 && rarity < RARITIES) {
			rarities.add(rarity);
		}

		// Most pets exist as legendary, fewer and fewer below that.
		for (int candidate : new int[] {4, 3, 2, 1, 0, 5}) {
			if (!rarities.contains(candidate)) {
				rarities.add(candidate);
			}
		}

		return rarities;
	}

	/** Reads the texture out of one item file. Returns null when the item is not a head with a texture. */
	static @Nullable String parseTexture(Reader reader) {
		JsonElement nbt = JsonParser.parseReader(reader).getAsJsonObject().get("nbttag");

		if (nbt == null) {
			return null;
		}

		Matcher matcher = TEXTURE.matcher(nbt.getAsString());
		return matcher.find() ? matcher.group("value") : null;
	}
}
