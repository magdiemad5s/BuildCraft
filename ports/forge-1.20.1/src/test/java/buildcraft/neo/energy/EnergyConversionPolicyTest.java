package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EnergyConversionPolicyTest {
    private static final long RATE = EnergyConversionPolicy.DEFAULT_MICRO_MJ_PER_FE;

    @Test
    void forgeEnergyToMjIsBoundedByEveryEndpoint() {
        assertEquals(
            new EnergyConversionPolicy.Transfer(40, 4_000_000L),
            EnergyConversionPolicy.forgeEnergyToMj(100, 40, 9_000_000L, RATE)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(12, 1_200_000L),
            EnergyConversionPolicy.forgeEnergyToMj(100, 40, 1_299_999L, RATE)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(7, 700_000L),
            EnergyConversionPolicy.forgeEnergyToMj(7, 40, 9_000_000L, RATE)
        );
    }

    @Test
    void mjToForgeEnergyIsBoundedByEveryEndpoint() {
        assertEquals(
            new EnergyConversionPolicy.Transfer(25, 2_500_000L),
            EnergyConversionPolicy.mjToForgeEnergy(9_000_000L, 25, 40, RATE)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(12, 1_200_000L),
            EnergyConversionPolicy.mjToForgeEnergy(1_299_999L, 100, 40, RATE)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(40, 4_000_000L),
            EnergyConversionPolicy.mjToForgeEnergy(9_000_000L, 100, 40, RATE)
        );
    }

    @Test
    void fractionalEnergyNeverCreatesAForgeEnergyUnit() {
        assertEquals(
            EnergyConversionPolicy.NONE,
            EnergyConversionPolicy.forgeEnergyToMj(1, 1, RATE - 1, RATE)
        );
        assertEquals(
            EnergyConversionPolicy.NONE,
            EnergyConversionPolicy.mjToForgeEnergy(RATE - 1, 1, 1, RATE)
        );
    }

    @Test
    void zeroAndNegativeEndpointsTransferNothing() {
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.forgeEnergyToMj(0, 1, RATE, RATE));
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.forgeEnergyToMj(1, 0, RATE, RATE));
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.forgeEnergyToMj(1, 1, 0, RATE));
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.mjToForgeEnergy(0, 1, 1, RATE));
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.mjToForgeEnergy(RATE, 0, 1, RATE));
        assertEquals(EnergyConversionPolicy.NONE, EnergyConversionPolicy.mjToForgeEnergy(RATE, 1, 0, RATE));
    }

    @Test
    void hugeRatesCannotOverflowMultiplication() {
        long hugeRate = Long.MAX_VALUE / 2;
        assertEquals(
            new EnergyConversionPolicy.Transfer(2, hugeRate * 2),
            EnergyConversionPolicy.forgeEnergyToMj(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, hugeRate)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(2, hugeRate * 2),
            EnergyConversionPolicy.mjToForgeEnergy(Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, hugeRate)
        );

        long overHalfLong = hugeRate + 1;
        assertEquals(
            new EnergyConversionPolicy.Transfer(1, overHalfLong),
            EnergyConversionPolicy.forgeEnergyToMj(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, overHalfLong)
        );
        assertEquals(
            new EnergyConversionPolicy.Transfer(1, overHalfLong),
            EnergyConversionPolicy.mjToForgeEnergy(Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, overHalfLong)
        );
    }

    @Test
    void invalidConversionRateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EnergyConversionPolicy.forgeEnergyToMj(1, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> EnergyConversionPolicy.mjToForgeEnergy(1, 1, 1, -1));
    }
}
