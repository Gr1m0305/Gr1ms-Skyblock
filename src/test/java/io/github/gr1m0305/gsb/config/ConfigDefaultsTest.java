package io.github.gr1m0305.gsb.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.github.notenoughupdates.moulconfig.annotations.Category;
import org.junit.jupiter.api.Test;

class ConfigDefaultsTest {
	/** Every feature has an "enabled" switch, and a fresh install starts with all of them off. */
	@Test
	void allFeaturesAreOffByDefault() throws ReflectiveOperationException {
		List<String> switches = new ArrayList<>();

		for (Field category : GsbConfig.class.getDeclaredFields()) {
			if (category.isAnnotationPresent(Category.class)) {
				collectSwitches(category.getType().getDeclaredConstructor().newInstance(), category.getName(), switches);
			}
		}

		// Guards against the check silently finding nothing after a rename.
		assertTrue(switches.size() >= 3, "found only these switches: " + switches);
	}

	private static void collectSwitches(Object options, String path, List<String> switches) throws ReflectiveOperationException {
		for (Field field : options.getClass().getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}

			field.setAccessible(true);
			String fieldPath = path + "." + field.getName();

			if (field.getType() == boolean.class && field.getName().equals("enabled")) {
				assertFalse(field.getBoolean(options), fieldPath + " must be off by default");
				switches.add(fieldPath);
			} else if (field.getType().getName().startsWith("io.github.gr1m0305.gsb.config.")) {
				// The options of a feature, grouped in a class of their own.
				collectSwitches(field.get(options), fieldPath, switches);
			}
		}
	}
}
