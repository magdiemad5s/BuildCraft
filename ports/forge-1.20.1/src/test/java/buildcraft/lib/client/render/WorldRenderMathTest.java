/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0
 */
package buildcraft.lib.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class WorldRenderMathTest {
    @Test
    void subtractsFarWorldCoordinatesBeforeNarrowingToFloat() {
        double origin = 29_999_984.0D;
        double worldCoordinate = origin + 0.4375D;

        assertEquals(0.4375F, WorldRenderMath.relativeAsFloat(worldCoordinate, origin));
        assertEquals(
            0.0F,
            (float) worldCoordinate - (float) origin,
            "Casting the absolute operands first demonstrates the precision regression this helper prevents"
        );
    }

    @Test
    void vectorPositionsStayTileLocalAtPositiveAndNegativeWorldLimits() {
        Vec3 renderOrigin = new Vec3(29_999_984.0D, 203.0D, -29_999_984.0D);
        Vec3 worldPosition = renderOrigin.add(0.4375D, 0.8125D, -0.21875D);

        Vec3 localPosition = WorldRenderMath.relative(worldPosition, renderOrigin);

        assertEquals(0.4375D, localPosition.x);
        assertEquals(0.8125D, localPosition.y);
        assertEquals(-0.21875D, localPosition.z);
        assertEquals(worldPosition.x, WorldRenderMath.worldCoordinate(renderOrigin.x, localPosition.x));
        assertEquals(worldPosition.y, WorldRenderMath.worldCoordinate(renderOrigin.y, localPosition.y));
        assertEquals(worldPosition.z, WorldRenderMath.worldCoordinate(renderOrigin.z, localPosition.z));
    }
}
