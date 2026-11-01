package com.errolmc.lightoverlay.client.overlay;

import com.errolmc.lightoverlay.client.config.OverlayConfig;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Whether the overlay is on, and the marks it draws. The marks are rescanned a few times a second on the client thread,
 * so newly placed torches and glowstone show up almost at once; the renderer only reads the latest list.
 */
public final class Overlay {
	private static final int SCAN_INTERVAL_TICKS = 5;

	private static boolean enabled;
	private static int ticksUntilScan;
	private static volatile List<Mark> marks = List.of();

	private Overlay() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		marks = List.of();
		rescanSoon();
	}

	/** Rescan on the next tick, for example after the settings changed. */
	public static void rescanSoon() {
		ticksUntilScan = 0;
	}

	public static List<Mark> marks() {
		return marks;
	}

	/** Called every client tick. */
	public static void tick(Minecraft minecraft) {
		if (!enabled || minecraft.level == null || minecraft.player == null) {
			marks = List.of();
			return;
		}
		if (--ticksUntilScan > 0) {
			return;
		}
		ticksUntilScan = SCAN_INTERVAL_TICKS;
		marks = List.copyOf(Scanner.scan(minecraft.level, minecraft.player.blockPosition(), OverlayConfig.get()));
	}
}
