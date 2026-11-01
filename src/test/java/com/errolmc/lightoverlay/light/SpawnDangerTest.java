package com.errolmc.lightoverlay.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpawnDangerTest {
	// Values from data/minecraft/dimension_type/*.json in 26.3.
	private static final int OVERWORLD_LIMIT = 0;
	private static final int OVERWORLD_MAX = 7;
	private static final int NETHER_LIMIT = 15;
	private static final int NETHER_MAX = 7;
	private static final int END_LIMIT = 0;
	private static final int END_MAX = 15;

	private static SpawnDanger overworld(int block, int sky) {
		return SpawnDanger.of(block, sky, OVERWORLD_LIMIT, OVERWORLD_MAX);
	}

	@Test
	void overworldAnyBlockLightIsSafe() {
		for (int sky = 0; sky <= 15; sky++) {
			for (int block = 1; block <= 15; block++) {
				assertEquals(SpawnDanger.SAFE, overworld(block, sky), "block " + block + ", sky " + sky);
			}
		}
	}

	@Test
	void overworldDarkCaveSpawnsAnyTime() {
		for (int sky = 0; sky <= 7; sky++) {
			assertEquals(SpawnDanger.ALWAYS, overworld(0, sky), "sky " + sky);
		}
	}

	@Test
	void overworldOpenSkySpawnsOnlyAtNight() {
		for (int sky = 8; sky <= 15; sky++) {
			assertEquals(SpawnDanger.NIGHT, overworld(0, sky), "sky " + sky);
		}
	}

	@Test
	void netherOnlyChecksTheLightLevel() {
		assertEquals(SpawnDanger.ALWAYS, SpawnDanger.of(0, 0, NETHER_LIMIT, NETHER_MAX));
		assertEquals(SpawnDanger.ALWAYS, SpawnDanger.of(7, 0, NETHER_LIMIT, NETHER_MAX));
		assertEquals(SpawnDanger.SAFE, SpawnDanger.of(8, 0, NETHER_LIMIT, NETHER_MAX));
	}

	@Test
	void endSpawnsWhereverBlockLightIsZero() {
		assertEquals(SpawnDanger.ALWAYS, SpawnDanger.of(0, 15, END_LIMIT, END_MAX));
		assertEquals(SpawnDanger.SAFE, SpawnDanger.of(1, 0, END_LIMIT, END_MAX));
	}

	@Test
	void worse() {
		assertEquals(SpawnDanger.ALWAYS, SpawnDanger.NIGHT.worse(SpawnDanger.ALWAYS));
		assertEquals(SpawnDanger.NIGHT, SpawnDanger.NIGHT.worse(SpawnDanger.SAFE));
		assertEquals(SpawnDanger.SAFE, SpawnDanger.SAFE.worse(SpawnDanger.SAFE));
	}
}
