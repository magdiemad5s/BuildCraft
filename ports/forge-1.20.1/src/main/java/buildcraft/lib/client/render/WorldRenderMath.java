/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * Precision-safe coordinate operations for world-space rendering.
 *
 * <p>JOML and Minecraft vertex buffers store positions as floats. World and camera/tile coordinates therefore have
 * to be subtracted while they are still doubles; casting either absolute operand first loses sub-block detail around
 * the coordinates used by large BuildCraft quarries and marker networks.</p>
 */
public final class WorldRenderMath {
    private WorldRenderMath() {
    }

    public static double relative(double worldCoordinate, double renderOrigin) {
        return worldCoordinate - renderOrigin;
    }

    public static float relativeAsFloat(double worldCoordinate, double renderOrigin) {
        return (float) relative(worldCoordinate, renderOrigin);
    }

    /**
     * Converts a world-space position to render-origin-local coordinates before any component is narrowed to a
     * float by a pose matrix or vertex buffer.
     */
    public static Vec3 relative(Vec3 worldPosition, Vec3 renderOrigin) {
        return new Vec3(
            relative(worldPosition.x, renderOrigin.x),
            relative(worldPosition.y, renderOrigin.y),
            relative(worldPosition.z, renderOrigin.z)
        );
    }

    /** Reconstructs the world coordinate used for lighting without routing the absolute value through a float. */
    public static double worldCoordinate(double renderOrigin, double relativeCoordinate) {
        return renderOrigin + relativeCoordinate;
    }
}
