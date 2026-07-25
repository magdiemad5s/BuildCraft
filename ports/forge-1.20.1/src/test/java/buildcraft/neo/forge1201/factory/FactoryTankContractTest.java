package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FactoryTankContractTest {
    @Test
    void preservesTheLegacyTankIdentityAndCapacity() {
        assertEquals("buildcraftfactory", FactoryTankContract.MODULE_ID);
        assertEquals("tank", FactoryTankContract.REGISTRY_PATH);
        assertEquals("tank", FactoryTankContract.FLUID_NBT_KEY);
        assertEquals(16_000, FactoryTankContract.CAPACITY_MILLIBUCKETS);
    }

    @Test
    void liquidAndGasTraversalRespectVerticalPhysics() {
        assertArrayEquals(new int[] {0, 1, 2}, VerticalTankPolicy.traversal(3, false, true));
        assertArrayEquals(new int[] {2, 1, 0}, VerticalTankPolicy.traversal(3, false, false));
        assertArrayEquals(new int[] {2, 1, 0}, VerticalTankPolicy.traversal(3, true, true));
        assertArrayEquals(new int[] {0, 1, 2}, VerticalTankPolicy.traversal(3, true, false));
    }

    @Test
    void comparatorNeverExceedsTheVanillaRange() {
        assertEquals(0, VerticalTankPolicy.comparatorLevel(0, 16_000));
        assertEquals(1, VerticalTankPolicy.comparatorLevel(1, 16_000));
        assertEquals(15, VerticalTankPolicy.comparatorLevel(16_000, 16_000));
        assertEquals(15, VerticalTankPolicy.comparatorLevel(50_000, 16_000));
        assertEquals(15, VerticalTankPolicy.comparatorLevel(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(0, VerticalTankPolicy.comparatorLevel(1, 0));
    }

    @Test
    void rejectsInvalidTankCounts() {
        assertThrows(IllegalArgumentException.class, () -> VerticalTankPolicy.traversal(-1, false, true));
    }
}
