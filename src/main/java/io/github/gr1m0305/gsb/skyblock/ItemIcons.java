package io.github.gr1m0305.gsb.skyblock;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.gr1m0305.gsb.data.ItemInfo;
import io.github.gr1m0305.gsb.data.LegacyMaterials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

/** Makes the items that GUI elements draw in front of their lines. */
public final class ItemIcons {
	/** Hypixel itself makes everything that has no item of its own out of paper. */
	private static final Item UNKNOWN = Items.PAPER;

	private ItemIcons() {
	}

	public static ItemStack of(Item item) {
		return new ItemStack(item);
	}

	/** An item that shines like an enchanted one. */
	public static ItemStack glowing(Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	/**
	 * What a SkyBlock item looks like, as far as the Hypixel API says.
	 *
	 * @param info the item, or null for one that is not in the item list
	 */
	public static ItemStack of(@Nullable ItemInfo info) {
		if (info == null) {
			return new ItemStack(UNKNOWN);
		}

		ItemInfo.Appearance appearance = info.appearance();
		Identifier name = Identifier.tryParse(LegacyMaterials.modernName(appearance.material(), appearance.durability()));
		Item item = name != null ? BuiltInRegistries.ITEM.getOptional(name).orElse(UNKNOWN) : UNKNOWN;
		ItemStack stack = appearance.glowing() ? glowing(item) : new ItemStack(item);

		if (item == Items.PLAYER_HEAD && !appearance.skin().isEmpty()) {
			stack.set(DataComponents.PROFILE, profile(appearance.skin()));
		}

		Identifier model = appearance.model().isEmpty() ? null : Identifier.tryParse(appearance.model());

		// These models are part of Hypixel's resource pack. Without the pack the item stays what it is made of.
		if (model != null && !(Minecraft.getInstance().getModelManager().getItemModel(model) instanceof MissingItemModel)) {
			stack.set(DataComponents.ITEM_MODEL, model);
		}

		return stack;
	}

	private static ResolvableProfile profile(String skin) {
		PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", skin)));
		// The same skin always gets the same profile, which is what the game caches the downloaded texture by.
		return ResolvableProfile.createResolved(new GameProfile(UUID.nameUUIDFromBytes(skin.getBytes(StandardCharsets.UTF_8)), "", properties));
	}
}
