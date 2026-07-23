package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BoundedPipeIngressTest {
    @Test
    void enforcesSharedCapacityAndPerSideIngressThroughput() {
        BoundedPipeIngress ingress = new BoundedPipeIngress(100, 30);

        assertEquals(0, ingress.receive(PipeSide.NORTH, 30, false));
        assertEquals(10, ingress.receive(PipeSide.NORTH, 10, false));
        assertEquals(0, ingress.receive(PipeSide.SOUTH, 30, false));
        assertEquals(60, ingress.stored());
        assertEquals(30, ingress.acceptedThisTick(PipeSide.NORTH));
        assertEquals(30, ingress.acceptedThisTick(PipeSide.SOUTH));

        ingress.advanceTick();
        assertEquals(0, ingress.acceptedThisTick(PipeSide.NORTH));
        assertEquals(0, ingress.receive(PipeSide.NORTH, 30, false));
        assertEquals(90, ingress.stored());
        assertEquals(20, ingress.receive(PipeSide.SOUTH, 30, false));
        assertEquals(100, ingress.stored());
    }

    @Test
    void simulationAndExecutionHaveMatchingIngressAccounting() {
        BoundedPipeIngress ingress = new BoundedPipeIngress(80, 50);

        assertEquals(0, ingress.receive(PipeSide.WEST, 50, true));
        assertEquals(0, ingress.stored());
        assertEquals(0, ingress.acceptedThisTick(PipeSide.WEST));
        assertEquals(0, ingress.receive(PipeSide.WEST, 50, false));
        assertEquals(50, ingress.stored());
        assertEquals(50, ingress.acceptedThisTick(PipeSide.WEST));

        assertEquals(20, ingress.receive(PipeSide.EAST, 50, true));
        assertEquals(50, ingress.stored());
        assertEquals(20, ingress.receive(PipeSide.EAST, 50, false));
        assertEquals(80, ingress.stored());
    }

    @Test
    void hookRetainsLongPrecisionWithoutBypassingBounds() {
        long offered = (long) Integer.MAX_VALUE + 40L;
        BoundedPipeIngress ingress = new BoundedPipeIngress(offered, offered);
        LongPowerIngressHook acceptEverything = (side, value, simulate) -> PowerHookResult.handled(value);

        assertEquals(0, ingress.receive(PipeSide.UP, offered, false, acceptEverything));
        assertEquals(offered, ingress.stored());
        assertEquals(offered, ingress.acceptedThisTick(PipeSide.UP));
    }

    @Test
    void rejectsMalformedHookAndProviderResponsesBeforeMutation() {
        BoundedPipeIngress ingress = new BoundedPipeIngress(100, 100);

        LongPowerIngressHook overAcceptingHook = (side, offered, simulate) -> PowerHookResult.handled(offered + 1);
        assertThrows(
            IllegalArgumentException.class,
            () -> ingress.receive(PipeSide.DOWN, 10, false, overAcceptingHook)
        );

        PassivePowerProvider invalidProvider = (minimum, maximum, simulate) -> maximum + 1;
        assertThrows(
            IllegalStateException.class,
            () -> ingress.pullFromPassive(PipeSide.DOWN, invalidProvider, 10, false)
        );
        assertEquals(0, ingress.stored());

        FeEnergyStorage invalidFeStorage = (maximum, simulate) -> maximum + 1;
        assertThrows(
            IllegalStateException.class,
            () -> ingress.pullFromFe(
                PipeSide.DOWN, invalidFeStorage, 1, new FeMicroJouleConversion(100), false
            )
        );
        assertEquals(0, ingress.stored());
    }

    @Test
    void pullsOnlyTheAmountPredictedByAWellBehavedPassiveProvider() {
        MicroJouleBuffer provider = new MicroJouleBuffer(200);
        provider.receive(75, false);
        BoundedPipeIngress ingress = new BoundedPipeIngress(100, 100);

        assertEquals(75, ingress.pullFromPassive(PipeSide.NORTH, provider, 90, true));
        assertEquals(75, provider.stored());
        assertEquals(0, ingress.stored());

        assertEquals(75, ingress.pullFromPassive(PipeSide.NORTH, provider, 90, false));
        assertEquals(0, provider.stored());
        assertEquals(75, ingress.stored());
    }

    @Test
    void neverExtractsOrCreatesSubFeEnergy() {
        MutableFeStore source = new MutableFeStore(1);
        BoundedPipeIngress ingress = new BoundedPipeIngress(99, 99);
        FeMicroJouleConversion conversion = new FeMicroJouleConversion(100);

        assertEquals(0, ingress.pullFromFe(PipeSide.EAST, source, 1, conversion, true));
        assertEquals(1, source.stored);
        assertEquals(0, ingress.stored());

        assertEquals(0, ingress.pullFromFe(PipeSide.EAST, source, 1, conversion, false));
        assertEquals(1, source.stored);
        assertEquals(0, ingress.stored());
    }

    private static final class MutableFeStore implements FeEnergyStorage {
        private int stored;

        private MutableFeStore(int stored) {
            this.stored = stored;
        }

        @Override
        public int extractEnergy(int maximum, boolean simulate) {
            int extracted = Math.min(stored, maximum);
            if (!simulate) {
                stored -= extracted;
            }
            return extracted;
        }
    }
}
