package io.github.gr1m0305.gsb.features.farming;

import java.util.HashMap;
import java.util.Map;

import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.skyblock.SkyblockItems;
import io.github.gr1m0305.gsb.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Compares the player's inventory with the previous tick and reports what was gained or lost. */
final class InventoryWatcher {
	/** After joining a world the server fills the inventory, which is not a gain. */
	private static final int WORLD_CHANGE_TICKS = 100;
	/** Menus can still hand out items for a moment after they close. */
	private static final int MENU_GRACE_TICKS = 20;

	private @Nullable LocalPlayer lastPlayer;
	private @Nullable Map<String, Integer> previous;
	private ItemStack[] slotStacks = new ItemStack[0];
	private String[] slotIds = new String[0];
	private int worldChangeTicks;
	private int menuGraceTicks;
	private boolean sackMenu;

	void tick(Minecraft minecraft, boolean tracking, FarmingTrackerLogic logic, long now) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			lastPlayer = null;
			previous = null;
			return;
		}

		if (player != lastPlayer) {
			lastPlayer = player;
			previous = null;
			worldChangeTicks = WORLD_CHANGE_TICKS;
		}

		Screen screen = GameCompat.screen(minecraft);
		boolean menuOpen = screen instanceof AbstractContainerScreen<?>;

		if (menuOpen) {
			menuGraceTicks = MENU_GRACE_TICKS;
			sackMenu = StringUtil.stripFormatting(screen.getTitle().getString()).contains("Sack");
		}

		Map<String, Integer> current = countItems(player.getInventory());

		if (previous != null && tracking && worldChangeTicks == 0) {
			for (Map.Entry<String, Integer> entry : current.entrySet()) {
				report(logic, entry.getKey(), entry.getValue() - previous.getOrDefault(entry.getKey(), 0), now);
			}

			for (Map.Entry<String, Integer> entry : previous.entrySet()) {
				if (!current.containsKey(entry.getKey())) {
					report(logic, entry.getKey(), -entry.getValue(), now);
				}
			}
		}

		previous = current;

		if (worldChangeTicks > 0) {
			worldChangeTicks--;
		}

		if (!menuOpen && menuGraceTicks > 0) {
			menuGraceTicks--;
		}
	}

	private void report(FarmingTrackerLogic logic, String id, int delta, long now) {
		if (delta == 0) {
			return;
		}

		if (menuGraceTicks == 0) {
			logic.inventoryChanged(id, delta, now);
		} else if (sackMenu && delta < 0) {
			// Whatever a menu gives or takes is not farming. Items put into a sack come back as a
			// "[Sacks]" message though, which must not count them a second time.
			logic.insertedIntoSack(id, -delta, now);
		}
	}

	private Map<String, Integer> countItems(Inventory inventory) {
		int size = inventory.getContainerSize();

		if (slotStacks.length != size) {
			slotStacks = new ItemStack[size];
			slotIds = new String[size];
		}

		Map<String, Integer> counts = new HashMap<>();

		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (stack.isEmpty()) {
				slotStacks[slot] = null;
				slotIds[slot] = null;
				continue;
			}

			// Reading the id copies the item's data, so only do it when the slot got a different stack.
			if (stack != slotStacks[slot]) {
				slotStacks[slot] = stack;
				slotIds[slot] = SkyblockItems.idOf(stack);
			}

			if (slotIds[slot] != null) {
				counts.merge(slotIds[slot], stack.getCount(), Integer::sum);
			}
		}

		return counts;
	}
}
