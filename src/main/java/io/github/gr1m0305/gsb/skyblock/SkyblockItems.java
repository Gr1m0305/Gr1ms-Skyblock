package io.github.gr1m0305.gsb.skyblock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

public final class SkyblockItems {
	private SkyblockItems() {
	}

	/** The SkyBlock id of an item, like {@code ENCHANTED_MELON}, or null when it has none. */
	public static @Nullable String idOf(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

		if (customData == null) {
			return null;
		}

		String id = customData.copyTag().getStringOr("id", "");
		return id.isEmpty() ? null : id;
	}
}
