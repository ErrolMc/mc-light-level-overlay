package com.errolmc.lightoverlay.client.input;

import com.errolmc.lightoverlay.client.config.DisplayMode;
import com.errolmc.lightoverlay.client.config.OverlayConfig;
import com.errolmc.lightoverlay.client.overlay.Overlay;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** H toggles the overlay; the mode key (unbound by default) switches between crosses and numbers. Both rebindable under Controls. */
public final class OverlayKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("light-level-overlay", "main"));

	public static final KeyMapping TOGGLE = new KeyMapping("key.light-level-overlay.toggle", InputConstants.KEY_H, CATEGORY);
	public static final KeyMapping MODE = new KeyMapping("key.light-level-overlay.mode", InputConstants.UNKNOWN.getValue(), CATEGORY);

	private OverlayKeys() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(TOGGLE);
		KeyMappingHelper.registerKeyMapping(MODE);
	}

	/** Called every client tick. */
	public static void tick(Minecraft minecraft) {
		while (TOGGLE.consumeClick()) {
			Overlay.setEnabled(!Overlay.isEnabled());
			announce(minecraft);
		}
		while (MODE.consumeClick()) {
			OverlayConfig config = OverlayConfig.get();
			config.displayMode = config.displayMode.next();
			OverlayConfig.save();
			Overlay.setEnabled(true);
			announce(minecraft);
		}
	}

	private static void announce(Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		DisplayMode mode = OverlayConfig.get().displayMode;
		minecraft.player.sendOverlayMessage(Overlay.isEnabled()
			? Component.translatable("light-level-overlay.message.on", mode.displayName())
			: Component.translatable("light-level-overlay.message.off"));
	}

	/** The toggle key's current binding, for example "H". */
	public static Component toggleKeyName() {
		return TOGGLE.getTranslatedKeyMessage();
	}
}
