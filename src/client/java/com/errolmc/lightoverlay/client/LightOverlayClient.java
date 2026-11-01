package com.errolmc.lightoverlay.client;

import com.errolmc.lightoverlay.client.config.OverlayConfig;
import com.errolmc.lightoverlay.client.input.OverlayKeys;
import com.errolmc.lightoverlay.client.overlay.Overlay;
import com.errolmc.lightoverlay.client.overlay.OverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class LightOverlayClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		OverlayConfig.load();
		OverlayKeys.register();
		OverlayRenderer.register();
		ClientTickEvents.END_CLIENT_TICK.register(OverlayKeys::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Overlay::tick);
	}
}
