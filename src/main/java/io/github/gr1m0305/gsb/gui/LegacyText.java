package io.github.gr1m0305.gsb.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class LegacyText {
	private LegacyText() {
	}

	/** Turns text with formatting codes like {@code §e§lTitle} into a text component. */
	public static Component parse(String text) {
		MutableComponent result = Component.empty();
		StringBuilder segment = new StringBuilder();
		Style style = Style.EMPTY;

		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			ChatFormatting formatting = character == '§' && i + 1 < text.length() ? ChatFormatting.getByCode(text.charAt(i + 1)) : null;

			if (formatting == null) {
				segment.append(character);
				continue;
			}

			if (!segment.isEmpty()) {
				result.append(Component.literal(segment.toString()).setStyle(style));
				segment.setLength(0);
			}

			// Like in vanilla, a colour code also ends bold, italic and so on.
			style = switch (formatting) {
				case RESET -> Style.EMPTY;
				case OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC -> style.applyFormat(formatting);
				default -> Style.EMPTY.withColor(formatting);
			};

			i++;
		}

		if (!segment.isEmpty()) {
			result.append(Component.literal(segment.toString()).setStyle(style));
		}

		return result;
	}
}
