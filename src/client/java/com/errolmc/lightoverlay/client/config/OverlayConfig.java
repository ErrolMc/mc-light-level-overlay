package com.errolmc.lightoverlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Settings, stored as {@code config/light-level-overlay.json}. */
public final class OverlayConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("light-level-overlay");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("light-level-overlay.json");

	public static final int MIN_HORIZONTAL_RANGE = 4;
	public static final int MAX_HORIZONTAL_RANGE = 48;
	public static final int MIN_VERTICAL_RANGE = 2;
	public static final int MAX_VERTICAL_RANGE = 32;

	private static OverlayConfig instance = new OverlayConfig();

	public DisplayMode displayMode = DisplayMode.CROSSES;

	/** Marks on water where drowned can spawn, at the surface above the darkest spot. */
	public boolean showWater = true;
	/** Yellow crosses where monsters only spawn at night (open sky); off shows only the always-dark (red) spots. */
	public boolean showNightSpawns = true;

	/** Numbers turn to face the way you look; off keeps them fixed, top of the digits to the north. */
	public boolean numbersFaceYou = true;

	/** Blocks out from the player, sideways and up/down. */
	public int horizontalRange = 16;
	public int verticalRange = 12;

	public static OverlayConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			instance = new OverlayConfig();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			OverlayConfig loaded = GSON.fromJson(reader, OverlayConfig.class);
			instance = loaded != null ? loaded.sanitized() : new OverlayConfig();
		} catch (IOException | JsonParseException e) {
			LOGGER.error("Could not read {}, using defaults", FILE, e);
			instance = new OverlayConfig();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Could not write {}", FILE, e);
		}
	}

	private OverlayConfig sanitized() {
		if (displayMode == null) {
			displayMode = DisplayMode.CROSSES;
		}
		horizontalRange = Mth.clamp(horizontalRange, MIN_HORIZONTAL_RANGE, MAX_HORIZONTAL_RANGE);
		verticalRange = Mth.clamp(verticalRange, MIN_VERTICAL_RANGE, MAX_VERTICAL_RANGE);
		return this;
	}
}
