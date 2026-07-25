/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.test.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;

class TemplateTargetPolicyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void waterAndLavaArePlacementTargetsRatherThanCompletedCells() {
        assertTrue(TemplateTargetPolicy.canPlaceInto(Blocks.WATER.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.countsAsFilled(Blocks.WATER.defaultBlockState()));
        assertTrue(TemplateTargetPolicy.canPlaceInto(Blocks.LAVA.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.countsAsFilled(Blocks.LAVA.defaultBlockState()));
    }

    @Test
    void replaceableSnowAndPlantsDoNotHideMissingTemplateBlocks() {
        assertTrue(TemplateTargetPolicy.canPlaceInto(Blocks.SNOW.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.countsAsFilled(Blocks.SNOW.defaultBlockState()));
        assertTrue(TemplateTargetPolicy.canPlaceInto(Blocks.TALL_GRASS.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.countsAsFilled(Blocks.TALL_GRASS.defaultBlockState()));
    }

    @Test
    void airCanBeFilledAndSolidBlocksAlreadySatisfyABooleanTemplateCell() {
        assertTrue(TemplateTargetPolicy.canPlaceInto(Blocks.AIR.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.countsAsFilled(Blocks.AIR.defaultBlockState()));
        assertFalse(TemplateTargetPolicy.canPlaceInto(Blocks.STONE.defaultBlockState()));
        assertTrue(TemplateTargetPolicy.countsAsFilled(Blocks.STONE.defaultBlockState()));
    }
}
