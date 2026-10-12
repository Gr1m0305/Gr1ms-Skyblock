package io.github.gr1m0305.gsb.features.pets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.gr1m0305.gsb.config.categories.GeneralCategory;
import io.github.gr1m0305.gsb.data.PetTextures;
import io.github.gr1m0305.gsb.gui.HudElement;
import io.github.gr1m0305.gsb.gui.HudLine;
import io.github.gr1m0305.gsb.gui.LegacyText;
import io.github.gr1m0305.gsb.gui.Position;
import io.github.gr1m0305.gsb.skyblock.ItemIcons;
import io.github.gr1m0305.gsb.skyblock.SkyblockState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Shows the pet that is out, its level and how far it is to the next one, everywhere on SkyBlock. */
public final class PetDisplay extends HudElement {
	private static final int WHITE = 0xFFFFFF;
	private static final int LEGENDARY = 0xFFAA00;

	private final Supplier<GeneralCategory.PetDisplay> config;
	private final PetTextures textures;
	private @Nullable List<String> readTabLines;
	/**
	 * What the Pet widget said when the tab list last had one. It is not in the tab list of every place, in
	 * dungeons for one, and the pet is still out there.
	 */
	private PetWidgetParser.@Nullable Widget widget;
	/** The colour of the pet's name in the tab list, which is the colour of its rarity. */
	private int color = WHITE;
	private PetWidgetParser.@Nullable Widget shownWidget;
	private int shownColor;
	private @Nullable String shownTexture;
	private @Nullable List<HudLine> lines;

	public PetDisplay(Supplier<GeneralCategory.PetDisplay> config, PetTextures textures) {
		super("Pet Display", GeneralCategory.PetDisplay::defaultPosition);
		this.config = config;
		this.textures = textures;
	}

	@Override
	public String title() {
		return "Pet";
	}

	@Override
	public Position position() {
		return config.get().position;
	}

	@Override
	public boolean isEnabled() {
		return config.get().enabled;
	}

	@Override
	public boolean isVisible() {
		return isEnabled() && SkyblockState.onSkyblock();
	}

	@Override
	public List<HudLine> lines() {
		readTabList();
		PetWidgetParser.Pet pet = widget != null ? widget.pet() : null;
		// Null until the texture is downloaded, the pet has a bone for its picture until then.
		String texture = pet != null ? textures.get(pet.name(), PetTextures.rarityOfColor(color)) : null;

		if (lines == null || !Objects.equals(widget, shownWidget) || color != shownColor || !Objects.equals(texture, shownTexture)) {
			shownWidget = widget;
			shownColor = color;
			shownTexture = texture;
			lines = buildLines(widget, color, texture);
		}

		return lines;
	}

	/** The editor is also open where no pet is known. */
	@Override
	public List<HudLine> previewLines() {
		readTabList();

		if (widget != null && widget.pet() != null) {
			return lines();
		}

		return buildLines(new PetWidgetParser.Widget(new PetWidgetParser.Pet("Mooshroom Cow", 87, new PetWidgetParser.Progress(0.613f, "61.3%")), 0),
				LEGENDARY, null);
	}

	/** The tab list is read into a new list a few times per second, only look at it when that happened. */
	private void readTabList() {
		List<String> tabLines = SkyblockState.tabLines();

		if (tabLines == readTabLines) {
			return;
		}

		readTabLines = tabLines;
		Optional<PetWidgetParser.Widget> read = PetWidgetParser.parse(tabLines);

		if (read.isEmpty()) {
			return;
		}

		widget = read.get();
		PetWidgetParser.Pet pet = widget.pet();

		if (pet != null) {
			color = colorOf(SkyblockState.tabComponents().get(widget.line()), pet.name());
		}
	}

	private static List<HudLine> buildLines(PetWidgetParser.@Nullable Widget widget, int color, @Nullable String texture) {
		if (widget == null) {
			return List.of(HudLine.of("§cNo Pet widget in the tab list").withIcon(ItemIcons.of(Items.BONE)));
		}

		PetWidgetParser.Pet pet = widget.pet();

		if (pet == null) {
			return List.of(HudLine.of("§7No pet selected").withIcon(ItemIcons.of(Items.BONE)));
		}

		ItemStack icon = texture != null ? ItemIcons.head(texture) : ItemIcons.of(Items.BONE);
		Component name = Component.empty()
				.append(LegacyText.parse("§7[Lvl " + pet.level() + "] "))
				.append(Component.literal(pet.name()).withColor(color));
		HudLine line = HudLine.of(name).withIcon(icon);
		PetWidgetParser.Progress progress = pet.progress();
		return List.of(progress != null ? line.withBar(progress.fraction(), progress.text()) : line);
	}

	/** The colour a piece of text has within a line of the tab list, white when it is not in that line. */
	private static int colorOf(Component line, String text) {
		StringBuilder plain = new StringBuilder();
		List<Integer> colors = new ArrayList<>();

		line.visit((style, piece) -> {
			TextColor styleColor = style.getColor();
			int color = styleColor != null ? styleColor.getValue() : WHITE;

			for (int i = 0; i < piece.length(); i++) {
				char character = piece.charAt(i);

				// Colours can also be written into the text as formatting codes.
				if (character == '§' && i + 1 < piece.length()) {
					ChatFormatting formatting = ChatFormatting.getByCode(piece.charAt(++i));
					TextColor codeColor = formatting != null ? TextColor.fromLegacyFormat(formatting) : null;

					if (codeColor != null) {
						color = codeColor.getValue();
					} else if (formatting == ChatFormatting.RESET) {
						color = WHITE;
					}

					continue;
				}

				plain.append(character);
				colors.add(color);
			}

			return Optional.empty();
		}, Style.EMPTY);

		int index = plain.indexOf(text);
		return index >= 0 ? colors.get(index) : WHITE;
	}
}
