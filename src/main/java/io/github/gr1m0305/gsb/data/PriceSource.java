package io.github.gr1m0305.gsb.data;

import org.jspecify.annotations.Nullable;

/** Which price an item is valued at. Every source falls back to the other one for items it has no price for. */
public enum PriceSource {
	NPC_SELL("NPC Sell Price") {
		@Override
		public double price(double npc, double instantSell, double sellOffer) {
			return npc > 0 ? npc : instantSell;
		}
	},
	BAZAAR_INSTANT_SELL("Bazaar Instant Sell") {
		@Override
		public double price(double npc, double instantSell, double sellOffer) {
			return instantSell > 0 ? instantSell : npc;
		}
	},
	BAZAAR_SELL_OFFER("Bazaar Sell Offer") {
		@Override
		public double price(double npc, double instantSell, double sellOffer) {
			return sellOffer > 0 ? sellOffer : npc;
		}
	},
	HIGHEST("Highest of NPC and Bazaar") {
		@Override
		public double price(double npc, double instantSell, double sellOffer) {
			return Math.max(npc, instantSell);
		}
	};

	private final String displayName;

	PriceSource(String displayName) {
		this.displayName = displayName;
	}

	protected abstract double price(double npc, double instantSell, double sellOffer);

	public double price(@Nullable ItemInfo item, BazaarPrices.@Nullable Price bazaar) {
		return price(item != null ? item.npcSellPrice() : 0, bazaar != null ? bazaar.instantSell() : 0, bazaar != null ? bazaar.sellOffer() : 0);
	}

	// The config menu shows this in the dropdown.
	@Override
	public String toString() {
		return displayName;
	}
}
