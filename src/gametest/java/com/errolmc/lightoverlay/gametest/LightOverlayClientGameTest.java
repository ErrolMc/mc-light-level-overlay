package com.errolmc.lightoverlay.gametest;

import com.errolmc.lightoverlay.client.config.DisplayMode;
import com.errolmc.lightoverlay.client.config.ModMenuIntegration;
import com.errolmc.lightoverlay.client.config.OverlayConfig;
import com.errolmc.lightoverlay.client.input.OverlayKeys;
import com.errolmc.lightoverlay.client.overlay.Mark;
import com.errolmc.lightoverlay.client.overlay.Overlay;
import com.errolmc.lightoverlay.light.SpawnDanger;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a sealed dark room with a torch in one corner and a 4-deep water basin with glowstone under one end in a test
 * world, turns the overlay on with the real H key and checks where the marks are. Screenshots of both modes land in
 * build/run/clientGameTest/screenshots.
 */
public class LightOverlayClientGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("light-level-overlay-gametest");

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(MuteAudio::mute);
		context.runOnClient(minecraft -> {
			OverlayConfig defaults = new OverlayConfig();
			OverlayConfig config = OverlayConfig.get();
			config.displayMode = DisplayMode.CROSSES;
			config.showWater = defaults.showWater;
			config.showNightSpawns = defaults.showNightSpawns;
			config.numbersFaceYou = defaults.numbersFaceYou;
			config.horizontalRange = defaults.horizontalRange;
			config.verticalRange = defaults.verticalRange;
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode creative @a");
			BlockPos spawn = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
			// The superflat grass is the floor; its top is one block under the player's feet.
			int floor = spawn.getY() - 1;
			Room room = buildRoom(world, new BlockPos(spawn.getX() + 4, floor, spawn.getZ() + 4));
			Basin basin = buildBasin(world, new BlockPos(spawn.getX() - 30, floor, spawn.getZ() + 4));
			context.waitTicks(20);

			check(!context.computeOnClient(minecraft -> Overlay.isEnabled()), "overlay starts on");
			darkRoom(context, world, room);
			water(context, world, basin);
			configScreen(context);

			pressToggle(context);
			check(!context.computeOnClient(minecraft -> Overlay.isEnabled()), "H did not turn the overlay off");
			check(marks(context).isEmpty(), "marks left after turning the overlay off");
			check(context.computeOnClient(minecraft -> minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).get()) == 0.0,
				"test client is not muted");
		}
	}

	// ------------------------------------------------------------------ scenarios

	/** A sealed 15x15 stone room: torch in one corner, and a glass floor block, a bottom slab and a carpet in the dark half. */
	private record Room(BlockPos corner, BlockPos torch, BlockPos nearTorch, BlockPos dark, BlockPos glass, BlockPos slab, BlockPos carpet) {
	}

	private static Room buildRoom(TestSingleplayerContext world, BlockPos corner) {
		BlockPos inside = corner.offset(1, 1, 1);
		world.getServer().runCommand("fill " + xyz(corner) + " " + xyz(corner.offset(16, 4, 16)) + " minecraft:stone hollow");
		Room room = new Room(corner, inside, inside.offset(1, 0, 0), inside.offset(13, 0, 13),
			inside.offset(11, 0, 7), inside.offset(11, 0, 9), inside.offset(11, 0, 11));
		setBlock(world, room.torch(), "minecraft:torch");
		setBlock(world, room.glass().below(), "minecraft:glass");
		setBlock(world, room.slab(), "minecraft:stone_slab[type=bottom]");
		setBlock(world, room.carpet(), "minecraft:white_carpet");
		return room;
	}

	private void darkRoom(ClientGameTestContext context, TestSingleplayerContext world, Room room) {
		teleport(context, world, room.torch().offset(1, 0, 1), -45, 30);
		pressToggle(context);
		check(context.computeOnClient(minecraft -> Overlay.isEnabled()), "H did not turn the overlay on");

		List<Mark> marks = marks(context);
		Mark dark = land(marks, room.dark()).orElseThrow(() -> new AssertionError("no cross in the dark corner of the room"));
		check(dark.danger() == SpawnDanger.ALWAYS && dark.light() == 0, "dark corner: " + dark);
		check(land(marks, room.nearTorch()).isEmpty(), "cross next to the torch");
		check(land(marks, room.glass()).isEmpty(), "cross on glass");
		check(land(marks, room.slab()).isEmpty() && land(marks, room.slab().above()).isEmpty(), "cross on a bottom slab");
		check(land(marks, room.carpet()).isEmpty(), "cross on a carpet");
		context.takeScreenshot("room-crosses");

		setMode(context, DisplayMode.NUMBERS);
		marks = marks(context);
		Mark lit = land(marks, room.nearTorch()).orElseThrow(() -> new AssertionError("no number next to the torch"));
		check(lit.danger() == SpawnDanger.SAFE && lit.light() == 13, "next to the torch: " + lit);
		dark = land(marks, room.dark()).orElseThrow(() -> new AssertionError("no number in the dark corner"));
		check(dark.danger() == SpawnDanger.ALWAYS && dark.light() == 0, "dark corner in numbers mode: " + dark);
		Mark glass = land(marks, room.glass()).orElseThrow(() -> new AssertionError("no number on glass"));
		check(glass.danger() == SpawnDanger.SAFE && glass.light() == 0, "glass is dark but nothing spawns on it: " + glass);
		Mark slab = land(marks, room.slab()).orElseThrow(() -> new AssertionError("no number on the slab"));
		check(slab.y() == room.slab().getY() + 0.5, "number not on top of the slab: " + slab);
		context.takeScreenshot("room-numbers");

		// Fixed numbers: the camera faces south-east, so the digits (top to the north) read at an angle.
		context.runOnClient(minecraft -> OverlayConfig.get().numbersFaceYou = false);
		context.waitTicks(2);
		context.takeScreenshot("room-numbers-fixed");
		context.runOnClient(minecraft -> OverlayConfig.get().numbersFaceYou = true);
		setMode(context, DisplayMode.CROSSES);
	}

	/** Stone basin, 5 wide, 24 long, 4 deep, open to the sky, with glowstone under the water at the near end. */
	private record Basin(BlockPos wall, BlockPos nearColumn, BlockPos farColumn, BlockPos grass) {
	}

	private static Basin buildBasin(TestSingleplayerContext world, BlockPos corner) {
		world.getServer().runCommand("fill " + xyz(corner) + " " + xyz(corner.offset(6, 4, 25)) + " minecraft:stone");
		world.getServer().runCommand("fill " + xyz(corner.offset(1, 1, 1)) + " " + xyz(corner.offset(5, 4, 24)) + " minecraft:water");
		setBlock(world, corner.offset(3, 0, 1), "minecraft:glowstone");
		// Columns are identified by the bottom water block (x and z are what matter).
		return new Basin(corner.offset(0, 5, 12), corner.offset(3, 1, 2), corner.offset(3, 1, 24), corner.offset(-2, 1, 12));
	}

	private void water(ClientGameTestContext context, TestSingleplayerContext world, Basin basin) {
		teleport(context, world, basin.wall(), -90, 40);
		List<Mark> marks = marks(context);
		logWaterColumn(marks, basin.nearColumn());
		logWaterColumn(marks, basin.farColumn());
		Mark far = water(marks, basin.farColumn()).orElseThrow(() -> new AssertionError("no cross on the dark end of the water"));
		check(far.danger() != SpawnDanger.SAFE && far.light() == 0, "dark end of the water: " + far);
		check(far.y() > basin.farColumn().getY() + 3 && far.y() < basin.farColumn().getY() + 4, "water cross not at the surface: " + far);
		check(water(marks, basin.nearColumn()).isEmpty(), "cross on the water over the glowstone");
		Mark grass = land(marks, basin.grass()).orElseThrow(() -> new AssertionError("no cross on the open grass"));
		check(grass.danger() == SpawnDanger.NIGHT, "open grass should only spawn at night: " + grass);
		context.takeScreenshot("water-crosses");

		setMode(context, DisplayMode.NUMBERS);
		marks = marks(context);
		Mark near = water(marks, basin.nearColumn()).orElseThrow(() -> new AssertionError("no number on the water over the glowstone"));
		check(near.danger() == SpawnDanger.SAFE && near.light() > 0, "water over the glowstone: " + near);
		far = water(marks, basin.farColumn()).orElseThrow(() -> new AssertionError("no number on the dark end of the water"));
		check(far.danger() != SpawnDanger.SAFE && far.light() == 0, "dark end of the water in numbers mode: " + far);
		context.takeScreenshot("water-numbers");

		// Without water marks, nothing is drawn on the water at all.
		context.runOnClient(minecraft -> OverlayConfig.get().showWater = false);
		rescan(context);
		check(marks(context).stream().noneMatch(Mark::water), "water marks with Show water off");
		context.runOnClient(minecraft -> OverlayConfig.get().showWater = true);
		setMode(context, DisplayMode.CROSSES);
	}

	private void configScreen(ClientGameTestContext context) {
		context.setScreen(() -> new ModMenuIntegration().getModConfigScreenFactory().create(null));
		context.waitTicks(5);
		context.takeScreenshot("config-screen");
		context.setScreen(() -> null);
	}

	// ------------------------------------------------------------------ helpers

	private static void logWaterColumn(List<Mark> marks, BlockPos column) {
		LOGGER.info("water column {} {}: {}", column.getX(), column.getZ(), water(marks, column).map(Mark::toString).orElse("no mark"));
	}

	private static Optional<Mark> land(List<Mark> marks, BlockPos pos) {
		return marks.stream()
			.filter(mark -> !mark.water() && at(mark, pos) && mark.y() >= pos.getY() && mark.y() < pos.getY() + 1)
			.findFirst();
	}

	private static Optional<Mark> water(List<Mark> marks, BlockPos column) {
		return marks.stream().filter(mark -> mark.water() && at(mark, column)).findFirst();
	}

	private static boolean at(Mark mark, BlockPos pos) {
		return (int) Math.floor(mark.x()) == pos.getX() && (int) Math.floor(mark.z()) == pos.getZ();
	}

	private static List<Mark> marks(ClientGameTestContext context) {
		return context.computeOnClient(minecraft -> Overlay.marks());
	}

	private static void pressToggle(ClientGameTestContext context) {
		context.getInput().pressKey(OverlayKeys.TOGGLE);
		context.waitTicks(10);
	}

	private static void setMode(ClientGameTestContext context, DisplayMode mode) {
		context.runOnClient(minecraft -> OverlayConfig.get().displayMode = mode);
		rescan(context);
	}

	private static void rescan(ClientGameTestContext context) {
		context.runOnClient(minecraft -> Overlay.rescanSoon());
		context.waitTicks(3);
	}

	private static void teleport(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos, float yaw, float pitch) {
		world.getServer().runCommand("tp @a " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() + 0.5) + " " + yaw + " " + pitch);
		world.getConnection().waitForChunksRender();
		context.waitTicks(10);
	}

	private static void setBlock(TestSingleplayerContext world, BlockPos pos, String block) {
		world.getServer().runCommand("setblock " + xyz(pos) + " " + block);
	}

	private static String xyz(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
