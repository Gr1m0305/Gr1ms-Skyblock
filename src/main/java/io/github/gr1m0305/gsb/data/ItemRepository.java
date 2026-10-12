package io.github.gr1m0305.gsb.data;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.gr1m0305.gsb.util.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Names, rarities, looks and NPC prices of all SkyBlock items, taken from the Hypixel API.
 * The last download is kept on disk so the data is there right after starting the game.
 */
public final class ItemRepository {
	private static final String ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static final long MAX_CACHE_AGE_MILLIS = 12 * 60 * 60 * 1000L;
	private static final long RETRY_INTERVAL_MILLIS = 60 * 1000L;

	private final Path cacheFile;
	private final AtomicBoolean downloading = new AtomicBoolean();
	private volatile long lastDownloadAttempt;
	private volatile Map<String, ItemInfo> byId = Map.of();
	private volatile Map<String, String> idByName = Map.of();

	public ItemRepository(Path cacheFile) {
		this.cacheFile = cacheFile;
	}

	/** Loads the cached data and refreshes it in the background when it is old or missing. */
	public void init() {
		boolean fresh = false;

		try {
			if (Files.exists(cacheFile)) {
				try (Reader reader = Files.newBufferedReader(cacheFile)) {
					setItems(parseCache(reader));
				}

				fresh = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis() < MAX_CACHE_AGE_MILLIS
						// A cache from before the mod drew item icons does not say what the items look like.
						&& byId.values().stream().anyMatch(item -> !item.appearance().material().isEmpty());
			}
		} catch (IOException | RuntimeException e) {
			GrimsSkyblock.LOGGER.warn("Could not read the item cache", e);
		}

		if (!fresh || byId.isEmpty()) {
			startDownload();
		}
	}

	/** Tries the download again when there still is no item data, at most once a minute. */
	public void retryIfMissing() {
		if (!isLoaded() && System.currentTimeMillis() - lastDownloadAttempt >= RETRY_INTERVAL_MILLIS) {
			startDownload();
		}
	}

	private void startDownload() {
		if (!downloading.compareAndSet(false, true)) {
			return;
		}

		lastDownloadAttempt = System.currentTimeMillis();
		HttpFetcher.EXECUTOR.execute(() -> {
			try {
				download();
			} finally {
				downloading.set(false);
			}
		});
	}

	private void download() {
		try {
			Map<String, ItemInfo> items = HttpFetcher.fetch(ITEMS_URL, ItemRepository::parseApiResponse);

			if (items.isEmpty()) {
				return;
			}

			setItems(items);
			Files.createDirectories(cacheFile.getParent());

			try (Writer writer = Files.newBufferedWriter(cacheFile)) {
				writer.write(toCacheJson(items).toString());
			}
		} catch (IOException | RuntimeException e) {
			GrimsSkyblock.LOGGER.warn("Could not download the SkyBlock item list", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	void setItems(Map<String, ItemInfo> items) {
		Map<String, String> names = new HashMap<>();

		for (ItemInfo item : items.values()) {
			String key = nameKey(item.name());
			String existing = names.get(key);

			// A few items share a name, prefer the one that can actually be sold.
			if (existing == null || (items.get(existing).npcSellPrice() <= 0 && item.npcSellPrice() > 0)) {
				names.put(key, item.id());
			}
		}

		this.byId = Map.copyOf(items);
		this.idByName = Map.copyOf(names);
	}

	public boolean isLoaded() {
		return !byId.isEmpty();
	}

	public @Nullable ItemInfo get(String id) {
		return byId.get(id);
	}

	/** Finds the item id for a display name as Hypixel prints it in chat. Unknown names get an id made from the name. */
	public String idFromName(String displayName) {
		String id = idByName.get(nameKey(displayName));

		if (id != null) {
			return id;
		}

		return StringUtil.stripFormatting(displayName).trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
	}

	public String displayName(String id) {
		ItemInfo info = byId.get(id);

		if (info != null) {
			return info.name();
		}

		// ENCHANTED_HAY_BLOCK becomes Enchanted Hay Block.
		StringBuilder builder = new StringBuilder();

		for (String word : id.toLowerCase(Locale.ROOT).split("[_:]+")) {
			if (word.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}

		return builder.toString();
	}

	private static String nameKey(String name) {
		return StringUtil.stripFormatting(name).trim().toLowerCase(Locale.ROOT);
	}

	/** Parses the response of {@code /v2/resources/skyblock/items}. */
	public static Map<String, ItemInfo> parseApiResponse(Reader reader) {
		JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
		Map<String, ItemInfo> items = new HashMap<>();

		for (JsonElement element : root.getAsJsonArray("items")) {
			JsonObject item = element.getAsJsonObject();

			if (!item.has("id") || !item.has("name")) {
				continue;
			}

			String id = item.get("id").getAsString();
			String name = StringUtil.stripFormatting(item.get("name").getAsString()).trim();
			String tier = item.has("tier") ? item.get("tier").getAsString() : "";
			double npcSellPrice = item.has("npc_sell_price") ? item.get("npc_sell_price").getAsDouble() : 0;
			// The texture of a head comes with a signature, which only matters to the server.
			JsonElement skin = item.get("skin") instanceof JsonObject skinObject ? skinObject.get("value") : null;
			ItemInfo.Appearance appearance = new ItemInfo.Appearance(string(item, "material"),
					item.has("durability") ? item.get("durability").getAsInt() : 0,
					item.has("glowing") && item.get("glowing").getAsBoolean(),
					skin != null ? skin.getAsString() : "",
					string(item, "item_model"));
			items.put(id, new ItemInfo(id, name, tier.isEmpty() ? "COMMON" : tier, npcSellPrice, appearance));
		}

		return items;
	}

	private static String string(JsonObject object, String key) {
		return object.has(key) ? object.get(key).getAsString() : "";
	}

	static JsonArray toCacheJson(Map<String, ItemInfo> items) {
		JsonArray array = new JsonArray();

		for (ItemInfo item : items.values()) {
			JsonObject object = new JsonObject();
			object.addProperty("id", item.id());
			object.addProperty("name", item.name());
			object.addProperty("tier", item.tier());
			object.addProperty("npc", item.npcSellPrice());
			ItemInfo.Appearance appearance = item.appearance();
			object.addProperty("material", appearance.material());

			// Most items have none of these, leaving them out keeps the file small.
			if (appearance.durability() != 0) {
				object.addProperty("durability", appearance.durability());
			}

			if (appearance.glowing()) {
				object.addProperty("glowing", true);
			}

			if (!appearance.skin().isEmpty()) {
				object.addProperty("skin", appearance.skin());
			}

			if (!appearance.model().isEmpty()) {
				object.addProperty("model", appearance.model());
			}

			array.add(object);
		}

		return array;
	}

	static Map<String, ItemInfo> parseCache(Reader reader) {
		Map<String, ItemInfo> items = new HashMap<>();

		for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
			JsonObject item = element.getAsJsonObject();
			String id = item.get("id").getAsString();
			ItemInfo.Appearance appearance = new ItemInfo.Appearance(string(item, "material"),
					item.has("durability") ? item.get("durability").getAsInt() : 0,
					item.has("glowing") && item.get("glowing").getAsBoolean(),
					string(item, "skin"),
					string(item, "model"));
			items.put(id, new ItemInfo(id, item.get("name").getAsString(), item.get("tier").getAsString(), item.get("npc").getAsDouble(), appearance));
		}

		return items;
	}
}
