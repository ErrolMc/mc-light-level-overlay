package com.errolmc.lightoverlay.light;

/**
 * Whether a light-sensitive monster (zombie, skeleton, creeper, drowned...) can spawn at a spot, judged from its light.
 * Mirrors vanilla's {@code Monster.isDarkEnoughToSpawn}; the random parts of that check decide how often, not whether.
 */
public enum SpawnDanger {
	/** Too bright: light-sensitive monsters never spawn here. */
	SAFE,
	/** Only dark enough at night or in a thunderstorm: open to the sky, so daylight is too bright. */
	NIGHT,
	/** Dark enough at any time of day. */
	ALWAYS;

	/** How far the sky light drops at midnight (thunderstorms drop it by 10). */
	static final int NIGHT_SKY_DARKEN = 11;

	/**
	 * @param blockLightLimit the dimension's {@code monster_spawn_block_light_limit}: 0 in the Overworld and the End,
	 *     15 (no block light check) in the Nether
	 * @param maxSpawnBrightness the highest value of the dimension's {@code monster_spawn_light_level}: 7 in the
	 *     Overworld and the Nether, 15 in the End
	 */
	public static SpawnDanger of(int blockLight, int skyLight, int blockLightLimit, int maxSpawnBrightness) {
		if (blockLightLimit < 15 && blockLight > blockLightLimit) {
			return SAFE;
		}
		// Vanilla compares max(block light, sky light minus the sky darkening) against the dimension's spawn light level.
		if (Math.max(blockLight, skyLight) <= maxSpawnBrightness) {
			return ALWAYS;
		}
		if (Math.max(blockLight, skyLight - NIGHT_SKY_DARKEN) <= maxSpawnBrightness) {
			return NIGHT;
		}
		return SAFE;
	}

	/** The more dangerous of the two. */
	public SpawnDanger worse(SpawnDanger other) {
		return other.ordinal() > ordinal() ? other : this;
	}
}
