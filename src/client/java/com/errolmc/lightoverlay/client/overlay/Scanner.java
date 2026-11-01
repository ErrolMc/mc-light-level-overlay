package com.errolmc.lightoverlay.client.overlay;

import com.errolmc.lightoverlay.client.config.DisplayMode;
import com.errolmc.lightoverlay.client.config.OverlayConfig;
import com.errolmc.lightoverlay.light.SpawnDanger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Finds the marks around the player, column by column, top to bottom.
 *
 * <p>Land spots use vanilla's own spawn checks for a zombie (the usual 1x2 monster): a floor monsters can spawn on
 * (not glass, leaves, bottom slabs...), room to stand, nothing in the way such as a carpet, then the light rule.
 *
 * <p>Water follows the drowned's rule: any water block with water below it and no solid block above it. Drowned can
 * spawn anywhere in the depth of a river, not just at the bottom, so each body of water gets one mark at its surface
 * that carries the darkest such spot underneath.
 */
final class Scanner {
	/** How far up to look for the surface when the scan starts inside a body of water. */
	private static final int MAX_SURFACE_SEARCH = 64;

	private final ClientLevel level;
	private final boolean numbers;
	private final boolean showWater;
	private final boolean showNightSpawns;
	private final int blockLightLimit;
	private final int maxSpawnBrightness;
	private final int top;
	private final int bottom;
	private final List<Mark> marks = new ArrayList<>();
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos other = new BlockPos.MutableBlockPos();

	// The body of water the column scan is in, if any.
	private boolean inWater;
	private double waterSurface;
	private boolean waterHasSpawnSpot;
	private int waterDarkest;
	private SpawnDanger waterDanger;

	private Scanner(ClientLevel level, BlockPos center, OverlayConfig config) {
		this.level = level;
		this.numbers = config.displayMode == DisplayMode.NUMBERS;
		this.showWater = config.showWater;
		this.showNightSpawns = config.showNightSpawns;
		DimensionType dimension = level.dimensionType();
		this.blockLightLimit = dimension.monsterSpawnBlockLightLimit();
		this.maxSpawnBrightness = dimension.monsterSpawnLightTest().maxInclusive();
		this.top = Math.min(center.getY() + config.verticalRange, level.getMaxY() - 1);
		this.bottom = Math.max(center.getY() - config.verticalRange, level.getMinY() + 1);
	}

	static List<Mark> scan(ClientLevel level, BlockPos center, OverlayConfig config) {
		Scanner scanner = new Scanner(level, center, config);
		int range = config.horizontalRange;
		for (int x = center.getX() - range; x <= center.getX() + range; x++) {
			for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
				int chunkX = SectionPos.blockToSectionCoord(x);
				int chunkZ = SectionPos.blockToSectionCoord(z);
				if (level.hasChunk(chunkX, chunkZ)) {
					scanner.column(level.getChunk(chunkX, chunkZ), x, z);
				}
			}
		}
		return scanner.marks;
	}

	private void column(LevelChunk chunk, int x, int z) {
		BlockState above = chunk.getBlockState(pos.set(x, top + 1, z));
		BlockState here = chunk.getBlockState(pos.set(x, top, z));
		inWater = false;
		for (int y = top; y >= bottom; y--) {
			BlockState below = chunk.getBlockState(pos.set(x, y - 1, z));
			if (here.getFluidState().is(FluidTags.WATER)) {
				if (showWater) {
					water(chunk, x, y, z, above, below);
				}
			} else {
				endWater(x, z);
				if (!below.isAir()) {
					land(x, y, z, here, below);
				}
			}
			above = here;
			here = below;
		}
		endWater(x, z);
	}

	private void land(int x, int y, int z, BlockState here, BlockState below) {
		pos.set(x, y, z);
		if (!here.getFluidState().isEmpty() || here.isCollisionShapeFullBlock(level, pos)) {
			return;
		}
		boolean spawnable = SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, EntityTypes.ZOMBIE)
			&& level.noCollision(EntityTypes.ZOMBIE.getSpawnAABB(x + 0.5, y, z + 0.5));
		if (!spawnable && !(numbers && below.isFaceSturdy(level, other.set(x, y - 1, z), Direction.UP))) {
			return;
		}
		int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
		SpawnDanger danger = spawnable ? danger(blockLight, level.getBrightness(LightLayer.SKY, pos)) : SpawnDanger.SAFE;
		if (numbers || danger != SpawnDanger.SAFE) {
			marks.add(new Mark(x + 0.5, y + coverHeight(here, pos), z + 0.5, blockLight, danger, false));
		}
	}

	private void water(LevelChunk chunk, int x, int y, int z, BlockState above, BlockState below) {
		if (!inWater) {
			inWater = true;
			waterSurface = surface(chunk, x, y, z);
			waterHasSpawnSpot = false;
			waterDarkest = 15;
			waterDanger = SpawnDanger.SAFE;
		}
		// Drowned need water below them, and (like every water mob) no solid block right above.
		if (!below.getFluidState().is(FluidTags.WATER) || above.isRedstoneConductor(level, other.set(x, y + 1, z))) {
			return;
		}
		pos.set(x, y, z);
		int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
		waterHasSpawnSpot = true;
		waterDarkest = Math.min(waterDarkest, blockLight);
		waterDanger = waterDanger.worse(danger(blockLight, level.getBrightness(LightLayer.SKY, pos)));
	}

	private void endWater(int x, int z) {
		if (inWater && waterHasSpawnSpot && (numbers || waterDanger != SpawnDanger.SAFE)) {
			marks.add(new Mark(x + 0.5, waterSurface, z + 0.5, waterDarkest, waterDanger, true));
		}
		inWater = false;
	}

	/** Height of the water surface above the top block of the body of water that contains (x, y, z). */
	private double surface(LevelChunk chunk, int x, int y, int z) {
		int surfaceY = y;
		BlockState state = chunk.getBlockState(pos.set(x, y, z));
		for (int i = 0; i < MAX_SURFACE_SEARCH && surfaceY < level.getMaxY(); i++) {
			BlockState up = chunk.getBlockState(pos.set(x, surfaceY + 1, z));
			if (!up.getFluidState().is(FluidTags.WATER)) {
				break;
			}
			surfaceY++;
			state = up;
		}
		return surfaceY + state.getFluidState().getOwnHeight();
	}

	private SpawnDanger danger(int blockLight, int skyLight) {
		SpawnDanger danger = SpawnDanger.of(blockLight, skyLight, blockLightLimit, maxSpawnBrightness);
		return danger == SpawnDanger.NIGHT && !showNightSpawns ? SpawnDanger.SAFE : danger;
	}

	/** Thin full-width blocks lying on the floor (snow, carpet, rails) would hide a mark drawn at floor level. */
	private double coverHeight(BlockState here, BlockPos at) {
		VoxelShape shape = here.getShape(level, at);
		if (shape.isEmpty()
			|| shape.min(Direction.Axis.X) > 0 || shape.max(Direction.Axis.X) < 1
			|| shape.min(Direction.Axis.Z) > 0 || shape.max(Direction.Axis.Z) < 1) {
			return 0;
		}
		return shape.max(Direction.Axis.Y);
	}
}
