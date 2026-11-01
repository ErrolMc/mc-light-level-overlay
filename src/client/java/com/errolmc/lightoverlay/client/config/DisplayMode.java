package com.errolmc.lightoverlay.client.config;

import net.minecraft.network.chat.Component;

/** What the overlay draws. */
public enum DisplayMode {
	/** A cross on every spot where monsters can spawn. */
	CROSSES,
	/** The block light level on every block you can stand on, every 0 in red. */
	NUMBERS;

	public Component displayName() {
		return Component.translatable("light-level-overlay.display_mode." + name().toLowerCase());
	}

	public DisplayMode next() {
		DisplayMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
