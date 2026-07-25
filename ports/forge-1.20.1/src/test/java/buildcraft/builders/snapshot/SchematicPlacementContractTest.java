/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0
 */
package buildcraft.builders.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SchematicPlacementContractTest {
    @Test
    void forgeEventRunsAfterProposedStateAndBeforeCopiedBlockEntityNbt() throws IOException {
        String placement = read("src/main/java/buildcraft/builders/snapshot/SchematicPlacementUtil.java");
        int snapshot = placement.indexOf("BlockSnapshot.create(");
        int proposedState = placement.indexOf("serverLevel.setBlock(blockPos, proposedState, flags)");
        int event = placement.indexOf("ForgeEventFactory.onBlockPlace(actor, replaced, placementDirection)");
        int restore = placement.indexOf("replaced.restore(true, true)");

        assertTrue(snapshot >= 0 && snapshot < proposedState);
        assertTrue(proposedState < event);
        assertTrue(event < restore);

        String schematic = read("src/main/java/buildcraft/builders/snapshot/SchematicBlockDefault.java");
        assertTrue(schematic.indexOf("SchematicPlacementUtil.place(") < schematic.indexOf("level.setBlockEntity(tileEntity)"));
        assertFalse(schematic.contains("boolean b = level.setBlock(blockPos, newBlockState, 11)"));
    }

    @Test
    void builderPassesOwnerContextAndFluidsUseTheModernEmptySentinel() throws IOException {
        String builder = read("src/main/java/buildcraft/builders/snapshot/BlueprintBuilder.java");
        assertTrue(builder.contains("tile.getOwner(),"));
        assertTrue(builder.contains("tile.getBuilderPos()"));

        String fluid = read("src/main/java/buildcraft/builders/snapshot/SchematicBlockFluid.java");
        assertTrue(fluid.contains("== Fluids.EMPTY"));
        assertTrue(fluid.contains(".filter(fluid -> fluid != Fluids.EMPTY)"));
        assertTrue(fluid.contains("SchematicPlacementUtil.place("));
        assertFalse(fluid.contains("BlockUtil.getFluid(context.world, context.pos) == null"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
