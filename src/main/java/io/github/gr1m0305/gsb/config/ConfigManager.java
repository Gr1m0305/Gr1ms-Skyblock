package io.github.gr1m0305.gsb.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.gr1m0305.gsb.GrimsSkyblock;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Loads and saves the config file and builds the config menu. */
public final class ConfigManager {
	private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
	private final Path file;
	private GsbConfig config = new GsbConfig();
	private @Nullable MoulConfigEditor<GsbConfig> editor;

	public ConfigManager(Path file) {
		this.file = file;
	}

	public GsbConfig get() {
		return config;
	}

	public void load() {
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				GsbConfig loaded = gson.fromJson(reader, GsbConfig.class);

				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | JsonParseException e) {
				GrimsSkyblock.LOGGER.error("Could not read {}, starting with the default settings", file, e);
				keepBrokenFile();
			}
		}

		config.sanitize();
	}

	/** Saving would overwrite a file that could not be read, so keep a copy for the user to fix. */
	private void keepBrokenFile() {
		try {
			Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			GrimsSkyblock.LOGGER.error("Could not back up {}", file, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temporary, gson.toJson(config));
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			GrimsSkyblock.LOGGER.error("Could not save {}", file, e);
		}
	}

	/** Creates the config menu. The menu itself is kept, so it reopens on the category it was left on. */
	public Screen createScreen() {
		if (editor == null) {
			MoulConfigProcessor<GsbConfig> processor = new MoulConfigProcessor<>(config);
			BuiltinMoulConfigGuis.addProcessors(processor);
			new ConfigProcessorDriver(processor).processConfig(config);
			editor = new MoulConfigEditor<>(processor);
		}

		return new MoulConfigScreenComponent(Component.empty(), new GuiContext(new GuiElementComponent(editor)), null);
	}
}
