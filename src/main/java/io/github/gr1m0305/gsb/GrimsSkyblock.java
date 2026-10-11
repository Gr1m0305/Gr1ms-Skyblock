package io.github.gr1m0305.gsb;

import java.nio.file.Path;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.compat.GameCompat;
import io.github.gr1m0305.gsb.config.ConfigManager;
import io.github.gr1m0305.gsb.data.BazaarPrices;
import io.github.gr1m0305.gsb.data.ItemRepository;
import io.github.gr1m0305.gsb.features.farming.FarmingFortuneDisplay;
import io.github.gr1m0305.gsb.features.farming.FarmingProfitTracker;
import io.github.gr1m0305.gsb.gui.HudManager;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrimsSkyblock implements ClientModInitializer {
	public static final String MOD_ID = "gr1msskyblock";
	public static final String GITHUB_URL = "https://github.com/Gr1m0305/Gr1ms-Skyblock";
	public static final Logger LOGGER = LoggerFactory.getLogger("Gr1msSkyblock");

	private static GrimsSkyblock instance;

	private ConfigManager config;
	private FarmingProfitTracker farmingTracker;
	private @Nullable Supplier<Screen> screenToOpen;

	public static GrimsSkyblock get() {
		return instance;
	}

	/** The mod version without the Minecraft version behind it. */
	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString().split("\\+")[0])
				.orElse("");
	}

	public ConfigManager config() {
		return config;
	}

	public FarmingProfitTracker farmingTracker() {
		return farmingTracker;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		Path directory = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

		config = new ConfigManager(directory.resolve("config.json"));
		config.load();

		ItemRepository items = new ItemRepository(directory.resolve("cache").resolve("items.json"));
		items.init();

		farmingTracker = new FarmingProfitTracker(() -> config.get().farming.profitTracker, items, new BazaarPrices(),
				directory.resolve("farming_tracker.json"));

		HudManager.register(farmingTracker.hud());
		HudManager.register(new FarmingFortuneDisplay(() -> config.get().farming.fortuneDisplay));
		HudManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> GsbCommand.register(dispatcher));
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
			farmingTracker.save();
			config.save();
		});
	}

	private void onTick(Minecraft minecraft) {
		SkyblockState.tick(minecraft);
		farmingTracker.tick(minecraft);

		if (screenToOpen != null) {
			Supplier<Screen> screen = screenToOpen;
			screenToOpen = null;
			GameCompat.setScreen(minecraft, screen.get());
		}
	}

	/** Commands run while the chat screen is still closing, so screens they open have to wait for the next tick. */
	public void openScreenNextTick(Supplier<Screen> screen) {
		screenToOpen = screen;
	}

	/** Called by the mixin for every chat message the server sends. */
	public static void onSystemChat(Component message) {
		if (instance == null || instance.farmingTracker == null) {
			return;
		}

		try {
			instance.farmingTracker.onChat(message);
		} catch (RuntimeException e) {
			// A message we fail to understand must never break the chat.
			LOGGER.error("Could not handle chat message", e);
		}
	}
}
