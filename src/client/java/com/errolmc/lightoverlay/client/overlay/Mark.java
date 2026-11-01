package com.errolmc.lightoverlay.client.overlay;

import com.errolmc.lightoverlay.light.SpawnDanger;

/**
 * One marker: the centre of a block's top surface (or a water surface), its block light, and whether monsters can spawn
 * there. For water, light and danger are those of the darkest spot below the surface where a drowned can spawn.
 *
 * @param danger {@link SpawnDanger#SAFE} also for spots monsters can't spawn on whatever the light (glass, slabs...)
 */
public record Mark(double x, double y, double z, int light, SpawnDanger danger, boolean water) {
}
