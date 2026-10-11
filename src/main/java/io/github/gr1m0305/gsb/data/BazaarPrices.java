package io.github.gr1m0305.gsb.data;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import org.jspecify.annotations.Nullable;

/** Current Bazaar prices from the Hypixel API. */
public final class BazaarPrices {
	private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";

	/**
	 * @param instantSell coins for selling one instantly (the highest buy order)
	 * @param sellOffer   coins for selling one through a sell offer (the lowest existing sell offer)
	 */
	public record Price(double instantSell, double sellOffer) {
	}

	private final AtomicBoolean fetching = new AtomicBoolean();
	private volatile Map<String, Price> prices = Map.of();
	private volatile long lastAttempt;

	public @Nullable Price get(String id) {
		return prices.get(id);
	}

	/** Downloads new prices in the background when the last download is older than {@code maxAgeMillis}. */
	public void refreshIfOlderThan(long maxAgeMillis) {
		if (System.currentTimeMillis() - lastAttempt < maxAgeMillis || !fetching.compareAndSet(false, true)) {
			return;
		}

		lastAttempt = System.currentTimeMillis();
		HttpFetcher.EXECUTOR.execute(() -> {
			try {
				Map<String, Price> downloaded = HttpFetcher.fetch(BAZAAR_URL, BazaarPrices::parseApiResponse);

				if (!downloaded.isEmpty()) {
					prices = Map.copyOf(downloaded);
				}
			} catch (IOException | RuntimeException e) {
				GrimsSkyblock.LOGGER.warn("Could not download Bazaar prices", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				fetching.set(false);
			}
		});
	}

	/** Parses the response of {@code /v2/skyblock/bazaar}. */
	public static Map<String, Price> parseApiResponse(Reader reader) {
		JsonObject products = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("products");
		Map<String, Price> prices = new HashMap<>();

		for (String id : products.keySet()) {
			JsonObject product = products.getAsJsonObject(id);
			JsonObject quickStatus = product.has("quick_status") ? product.getAsJsonObject("quick_status") : new JsonObject();
			// "sell_summary" lists the buy orders someone selling would fill, "buy_summary" the sell offers.
			double instantSell = topPrice(product, "sell_summary", quickStatus, "sellPrice");
			double sellOffer = topPrice(product, "buy_summary", quickStatus, "buyPrice");
			prices.put(id, new Price(instantSell, sellOffer));
		}

		return prices;
	}

	private static double topPrice(JsonObject product, String summaryKey, JsonObject quickStatus, String quickStatusKey) {
		if (product.has(summaryKey)) {
			JsonArray summary = product.getAsJsonArray(summaryKey);

			if (!summary.isEmpty()) {
				return summary.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble();
			}
		}

		return quickStatus.has(quickStatusKey) ? quickStatus.get(quickStatusKey).getAsDouble() : 0;
	}
}
