package buildcraft.energy.generation.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.Test;

import buildcraft.energy.generation.features.OilGenerationPolicy.DepositType;

class OilGenerationPolicyTest {
    @Test
    void everyLegacyDepositSizeIsReachableWithDefaultProbabilities() {
        assertEquals(
            DepositType.LARGE,
            OilGenerationPolicy.select(sequence(0.0), 1.0, 0.04, 0.1, 2.0)
        );
        assertEquals(
            DepositType.MEDIUM,
            OilGenerationPolicy.select(sequence(0.5, 0.0), 1.0, 0.04, 0.1, 2.0)
        );
        assertEquals(
            DepositType.LAKE,
            OilGenerationPolicy.select(sequence(0.5, 0.5, 0.0), 1.0, 0.04, 0.1, 2.0)
        );
    }

    @Test
    void smallDepositsRemainReachableWithoutAConfiguredSurfaceBiomeList() {
        assertEquals(
            DepositType.LAKE,
            OilGenerationPolicy.select(sequence(0.0), 1.0, 0.0, 0.0, 2.0)
        );
    }

    @Test
    void largerDepositRollsRetainPriority() {
        assertEquals(
            DepositType.LARGE,
            OilGenerationPolicy.select(sequence(0.0), 1.0, 0.04, 100.0, 100.0)
        );
        assertEquals(
            DepositType.MEDIUM,
            OilGenerationPolicy.select(sequence(0.99, 0.0), 1.0, 0.04, 0.1, 100.0)
        );
    }

    @Test
    void zeroProbabilitiesCannotGenerateOnAnExactZeroRoll() {
        assertEquals(
            DepositType.NONE,
            OilGenerationPolicy.select(sequence(0.0, 0.0, 0.0), 1.0, 0.0, 0.0, 0.0)
        );
    }

    @Test
    void invalidOrDisabledGenerationMultiplierProducesNoDeposit() {
        assertEquals(
            DepositType.NONE,
            OilGenerationPolicy.select(sequence(0.0), 0.0, 100.0, 100.0, 100.0)
        );
        assertEquals(
            DepositType.NONE,
            OilGenerationPolicy.select(sequence(0.0), Double.NaN, 100.0, 100.0, 100.0)
        );
    }

    @Test
    void blacklistAndWhitelistSemanticsMatchTheLegacyGenerator() {
        assertEquals(false, OilGenerationPolicy.isAllowedByList(true, true));
        assertEquals(true, OilGenerationPolicy.isAllowedByList(false, true));
        assertEquals(true, OilGenerationPolicy.isAllowedByList(true, false));
        assertEquals(false, OilGenerationPolicy.isAllowedByList(false, false));
    }

    @Test
    void legacyDesertAndOceanTogglesControlTheirModernExcessiveBiomes() {
        assertEquals(
            false,
            OilGenerationPolicy.isLegacyExcessiveBiomeEnabled("minecraft", "desert", false, true)
        );
        assertEquals(
            false,
            OilGenerationPolicy.isLegacyExcessiveBiomeEnabled("minecraft", "deep_ocean", true, false)
        );
        assertEquals(
            true,
            OilGenerationPolicy.isLegacyExcessiveBiomeEnabled("example", "deep_ocean", false, false)
        );
    }

    @Test
    void runtimeAndDatapackRatesLayerWithoutChangingEitherDefault() {
        assertEquals(2.0, OilGenerationPolicy.combineGenerationRates(2.0, 1.0));
        assertEquals(2.0, OilGenerationPolicy.combineGenerationRates(1.0, 2.0));
        assertEquals(0.0, OilGenerationPolicy.combineGenerationRates(1.0, 0.0));

        assertEquals(5.0, OilGenerationPolicy.layerPercentage(5.0, 2.0, 2.0));
        assertEquals(4.0, OilGenerationPolicy.layerPercentage(2.0, 4.0, 2.0));
        assertEquals(0.0, OilGenerationPolicy.layerPercentage(2.0, 0.0, 2.0));
    }

    @Test
    void runtimeAndDatapackSpoutHeightsLayerAndNormalize() {
        assertEquals(8, OilGenerationPolicy.layerHeight(8, 6, 6));
        assertEquals(9, OilGenerationPolicy.layerHeight(6, 9, 6));
        assertEquals(0, OilGenerationPolicy.layerHeight(0, 0, 6));
        assertEquals(256, OilGenerationPolicy.layerHeight(256, 256, 6));

        assertEquals(12, OilGenerationPolicy.chooseInclusiveHeight(bound -> {
            assertEquals(7, bound);
            return 6;
        }, 12, 6));
        assertEquals(10, OilGenerationPolicy.chooseInclusiveHeight(bound -> {
            throw new AssertionError("A fixed height must not consume randomness");
        }, 10, 10));
    }

    @Test
    void disablingVisibleSpoutsCannotInflateTheLargeSpringTubeToReservoirSize() {
        assertEquals(1, OilGenerationPolicy.springTubeRadius(DepositType.LARGE));
        assertEquals(0, OilGenerationPolicy.springTubeRadius(DepositType.MEDIUM));
        assertEquals(0, OilGenerationPolicy.springTubeRadius(DepositType.LAKE));
    }

    private static DoubleSupplier sequence(double... values) {
        Deque<Double> remaining = new ArrayDeque<>();
        for (double value : values) {
            remaining.addLast(value);
        }
        return () -> {
            if (remaining.isEmpty()) {
                throw new AssertionError("Oil policy requested more random rolls than expected");
            }
            return remaining.removeFirst();
        };
    }
}