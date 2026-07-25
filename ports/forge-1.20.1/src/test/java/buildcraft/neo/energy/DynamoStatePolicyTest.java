package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DynamoStatePolicyTest {
    @Test
    void persistedBuffersAreBounded() {
        assertEquals(0, DynamoStatePolicy.clampStoredForgeEnergy(-1, 10_000));
        assertEquals(7_500, DynamoStatePolicy.clampStoredForgeEnergy(7_500, 10_000));
        assertEquals(10_000, DynamoStatePolicy.clampStoredForgeEnergy(Integer.MAX_VALUE, 10_000));

        assertEquals(0L, DynamoStatePolicy.clampStoredMicroJoules(-1, 1_000_000L));
        assertEquals(750_000L, DynamoStatePolicy.clampStoredMicroJoules(750_000L, 1_000_000L));
        assertEquals(1_000_000L, DynamoStatePolicy.clampStoredMicroJoules(Long.MAX_VALUE, 1_000_000L));
    }

    @Test
    void animationAndHeatRejectInvalidNbtValues() {
        assertEquals(20.0, DynamoStatePolicy.clampHeat(Double.NaN, 20.0, 250.0));
        assertEquals(20.0, DynamoStatePolicy.clampHeat(-100.0, 20.0, 250.0));
        assertEquals(250.0, DynamoStatePolicy.clampHeat(1_000.0, 20.0, 250.0));
        assertEquals(0.0F, DynamoStatePolicy.clampProgress(Float.NaN));
        assertEquals(0.0F, DynamoStatePolicy.clampProgress(-1.0F));
        assertEquals(1.0F, DynamoStatePolicy.clampProgress(2.0F));
        assertEquals(0, DynamoStatePolicy.clampProgressPart(-1));
        assertEquals(2, DynamoStatePolicy.clampProgressPart(99));
    }

    @Test
    void receiverCannotCreateOrOverdrawForgeEnergy() {
        assertEquals(0, DynamoStatePolicy.acceptedForgeEnergy(100, -1));
        assertEquals(40, DynamoStatePolicy.acceptedForgeEnergy(100, 40));
        assertEquals(100, DynamoStatePolicy.acceptedForgeEnergy(100, 101));
        assertEquals(0, DynamoStatePolicy.acceptedForgeEnergy(0, 10));
    }

    @Test
    void invalidBoundsAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DynamoStatePolicy.clampStoredForgeEnergy(0, -1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> DynamoStatePolicy.clampStoredMicroJoules(0, -1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> DynamoStatePolicy.clampHeat(20.0, 250.0, 20.0)
        );
    }
}
