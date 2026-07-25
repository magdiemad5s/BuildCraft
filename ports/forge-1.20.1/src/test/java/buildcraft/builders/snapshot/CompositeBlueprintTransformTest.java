package buildcraft.builders.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

class CompositeBlueprintTransformTest {
    @Test
    void convertsWorldCellsRelativeToCompositeMinimum() {
        assertEquals(
            new BlockPos(9, 5, 12),
            CompositeBlueprintCapture.toCompositePosition(
                new BlockPos(4, 70, 20),
                new BlockPos(-5, 65, 8)
            )
        );
    }

    @Test
    void linkedBlueprintBuildingTransformRoundTripsClockwiseRotation() {
        Blueprint blueprint = new Blueprint();
        blueprint.size = new BlockPos(2, 1, 3);
        blueprint.offset = BlockPos.ZERO;
        blueprint.facing = Direction.NORTH;
        blueprint.palette.add(new SchematicBlockIgnore());
        blueprint.data = new int[Snapshot.getDataSize(blueprint.size)];

        BlockPos base = new BlockPos(20, 64, 30);
        Blueprint.BuildingInfo info = blueprint.new BuildingInfo(base, Rotation.CLOCKWISE_90, null);
        BlockPos local = new BlockPos(1, 0, 2);
        BlockPos expectedWorld = new BlockPos(18, 64, 31);

        assertEquals(expectedWorld, info.toWorld(local));
        assertEquals(local, info.fromWorld(expectedWorld));
        assertEquals(new BlockPos(18, 64, 30), info.box.min());
        assertEquals(new BlockPos(20, 64, 31), info.box.max());
    }

    @Test
    void sparseGapSchematicNeverExcavatesOrPlaces() {
        SchematicBlockIgnore ignore = new SchematicBlockIgnore();

        assertTrue(ignore.isBuilt(null, BlockPos.ZERO));
        assertFalse(ignore.isAir());
        assertFalse(ignore.canBuild(null, BlockPos.ZERO));
        assertTrue(ignore.build(null, BlockPos.ZERO));
        assertTrue(ignore.buildWithoutChecks(null, BlockPos.ZERO));
    }
}
