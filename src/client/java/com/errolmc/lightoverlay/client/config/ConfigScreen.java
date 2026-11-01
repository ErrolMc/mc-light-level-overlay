package com.errolmc.lightoverlay.client.config;

import com.errolmc.lightoverlay.client.input.OverlayKeys;
import com.errolmc.lightoverlay.client.overlay.Overlay;
import java.util.Arrays;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The Cloth Config settings screen. Only loaded when Cloth Config is installed. */
final class ConfigScreen {
	private ConfigScreen() {
	}

	static Screen create(Screen parent) {
		OverlayConfig config = OverlayConfig.get();
		OverlayConfig defaults = new OverlayConfig();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("light-level-overlay.config.title"))
			.setSavingRunnable(() -> {
				OverlayConfig.save();
				Overlay.rescanSoon();
			});
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory overlay = builder.getOrCreateCategory(Component.translatable("light-level-overlay.config.category.overlay"));
		overlay.addEntry(entries.startTextDescription(Component.translatable("light-level-overlay.config.keys_info", OverlayKeys.toggleKeyName())).build());
		overlay.addEntry(entries.startEnumSelector(text("display_mode"), DisplayMode.class, config.displayMode)
			.setDefaultValue(defaults.displayMode)
			.setEnumNameProvider(mode -> ((DisplayMode) mode).displayName())
			.setTooltip(tooltip("display_mode"))
			.setSaveConsumer(value -> config.displayMode = value)
			.build());
		overlay.addEntry(entries.startBooleanToggle(text("show_water"), config.showWater)
			.setDefaultValue(defaults.showWater)
			.setTooltip(tooltip("show_water"))
			.setSaveConsumer(value -> config.showWater = value)
			.build());
		overlay.addEntry(entries.startBooleanToggle(text("show_night_spawns"), config.showNightSpawns)
			.setDefaultValue(defaults.showNightSpawns)
			.setTooltip(tooltip("show_night_spawns"))
			.setSaveConsumer(value -> config.showNightSpawns = value)
			.build());
		overlay.addEntry(entries.startBooleanToggle(text("numbers_face_you"), config.numbersFaceYou)
			.setDefaultValue(defaults.numbersFaceYou)
			.setTooltip(tooltip("numbers_face_you"))
			.setSaveConsumer(value -> config.numbersFaceYou = value)
			.build());
		overlay.addEntry(entries.startIntSlider(text("horizontal_range"), config.horizontalRange,
				OverlayConfig.MIN_HORIZONTAL_RANGE, OverlayConfig.MAX_HORIZONTAL_RANGE)
			.setDefaultValue(defaults.horizontalRange)
			.setTooltip(tooltip("horizontal_range"))
			.setSaveConsumer(value -> config.horizontalRange = value)
			.build());
		overlay.addEntry(entries.startIntSlider(text("vertical_range"), config.verticalRange,
				OverlayConfig.MIN_VERTICAL_RANGE, OverlayConfig.MAX_VERTICAL_RANGE)
			.setDefaultValue(defaults.verticalRange)
			.setTooltip(tooltip("vertical_range"))
			.setSaveConsumer(value -> config.verticalRange = value)
			.build());

		return builder.build();
	}

	private static Component text(String key) {
		return Component.translatable("light-level-overlay.config." + key);
	}

	/** Cloth Config does not wrap tooltips, so the translations put a newline where each line should end. */
	private static Component[] tooltip(String key) {
		String text = Component.translatable("light-level-overlay.config." + key + ".tooltip").getString();
		return Arrays.stream(text.split("\n")).map(Component::literal).toArray(Component[]::new);
	}
}
