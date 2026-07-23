package buildcraft.neo.energy;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Finite, server-thread-confined MJ ingress for one powered pipe.
 *
 * <p>The legacy pipe sections accepted any positive MJ amount and reported
 * full acceptance during simulation. This class provides one finite shared
 * buffer plus a separately enforced ingress budget for every pipe side. Call
 * {@link #advanceTick()} exactly once at the start of the pipe's server tick.
 * All receive methods return excess MJ, matching the legacy receiver
 * convention.</p>
 */
public final class BoundedPipeIngress {
    private final long capacity;
    private final long maximumIngressPerSidePerTick;
    private final EnumMap<PipeSide, Long> acceptedThisTick = new EnumMap<>(PipeSide.class);
    private long stored;

    public BoundedPipeIngress(long capacity, long maximumIngressPerSidePerTick) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative: " + capacity);
        }
        if (maximumIngressPerSidePerTick < 0) {
            throw new IllegalArgumentException(
                "maximumIngressPerSidePerTick must not be negative: " + maximumIngressPerSidePerTick
            );
        }
        this.capacity = capacity;
        this.maximumIngressPerSidePerTick = maximumIngressPerSidePerTick;
        for (PipeSide side : PipeSide.values()) {
            acceptedThisTick.put(side, 0L);
        }
    }

    public long capacity() {
        return capacity;
    }

    public long maximumIngressPerSidePerTick() {
        return maximumIngressPerSidePerTick;
    }

    public long stored() {
        return stored;
    }

    public long acceptedThisTick(PipeSide side) {
        return acceptedThisTick.get(requireSide(side));
    }

    /** Resets only the per-side ingress budgets; stored MJ remains intact. */
    public void advanceTick() {
        for (PipeSide side : PipeSide.values()) {
            acceptedThisTick.put(side, 0L);
        }
    }

    /**
     * Offers MJ using the default bounded pipe behavior.
     *
     * @return MJ that was not accepted.
     */
    public long receive(PipeSide side, long offeredMicroJoules, boolean simulate) {
        side = requireSide(side);
        requireNonNegative(offeredMicroJoules, "offeredMicroJoules");
        long accepted = maximumAcceptable(side, offeredMicroJoules);
        if (!simulate) {
            commit(side, accepted);
        }
        return offeredMicroJoules - accepted;
    }

    /**
     * Offers MJ after evaluating a long-precision hook. The hook can choose a
     * smaller accepted amount, but cannot exceed the normal pipe bounds.
     *
     * @return MJ that was not accepted.
     */
    public long receive(PipeSide side, long offeredMicroJoules, boolean simulate, LongPowerIngressHook hook) {
        side = requireSide(side);
        requireNonNegative(offeredMicroJoules, "offeredMicroJoules");
        Objects.requireNonNull(hook, "hook");

        PowerHookResult result = Objects.requireNonNull(
            hook.receive(side, offeredMicroJoules, simulate),
            "hook result"
        );
        if (!result.handled()) {
            return receive(side, offeredMicroJoules, simulate);
        }

        long accepted = result.acceptedFrom(offeredMicroJoules);
        long maximum = maximumAcceptable(side, offeredMicroJoules);
        if (accepted > maximum) {
            throw new IllegalStateException(
                "power hook accepted " + accepted + " MJ outside pipe bounds [0, " + maximum + "]"
            );
        }
        if (!simulate) {
            commit(side, accepted);
        }
        return offeredMicroJoules - accepted;
    }

    /**
     * Pulls MJ from a passive provider without exceeding this pipe's current
     * capacity or side throughput. Provider simulation and execution responses
     * are validated before any pipe state is changed.
     *
     * @return MJ actually extracted and accepted by the pipe.
     */
    public long pullFromPassive(
        PipeSide side, PassivePowerProvider provider, long maximumMicroJoules, boolean simulate
    ) {
        side = requireSide(side);
        Objects.requireNonNull(provider, "provider");
        requireNonNegative(maximumMicroJoules, "maximumMicroJoules");

        long maximum = maximumAcceptable(side, maximumMicroJoules);
        if (maximum == 0) {
            return 0;
        }

        long predicted = validateExternalAmount(
            provider.extract(0, maximum, true), maximum, "simulated passive extraction"
        );
        if (simulate || predicted == 0) {
            return predicted;
        }

        long extracted = validateExternalAmount(
            provider.extract(0, predicted, false), predicted, "passive extraction"
        );
        commit(side, extracted);
        return extracted;
    }

    /**
     * Pulls exact whole-FE amounts from an FE source. If the remaining pipe
     * capacity is smaller than one FE unit, this returns zero and leaves the
     * source untouched rather than creating energy from a fractional unit.
     *
     * @return FE actually extracted and represented in the pipe buffer.
     */
    public int pullFromFe(
        PipeSide side,
        FeEnergyStorage storage,
        int maximumForgeEnergy,
        FeMicroJouleConversion conversion,
        boolean simulate
    ) {
        side = requireSide(side);
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(conversion, "conversion");
        if (maximumForgeEnergy < 0) {
            throw new IllegalArgumentException("maximumForgeEnergy must not be negative: " + maximumForgeEnergy);
        }

        long maximumMicroJoules = conversion.microJoulesForFe(maximumForgeEnergy);
        long room = maximumAcceptable(side, maximumMicroJoules);
        int request = conversion.feForMicroJoules(room, maximumForgeEnergy);
        if (request == 0) {
            return 0;
        }

        int predicted = validateExternalAmount(
            storage.extractEnergy(request, true), request, "simulated FE extraction"
        );
        if (simulate || predicted == 0) {
            return predicted;
        }

        int extracted = validateExternalAmount(
            storage.extractEnergy(predicted, false), predicted, "FE extraction"
        );
        commit(side, conversion.microJoulesForFe(extracted));
        return extracted;
    }

    /** Removes up to the requested MJ after downstream transport succeeds. */
    public long drain(long maximumMicroJoules, boolean simulate) {
        requireNonNegative(maximumMicroJoules, "maximumMicroJoules");
        long drained = Math.min(stored, maximumMicroJoules);
        if (!simulate) {
            stored -= drained;
        }
        return drained;
    }

    private long maximumAcceptable(PipeSide side, long offeredMicroJoules) {
        long capacityRemaining = capacity - stored;
        long sideRemaining = maximumIngressPerSidePerTick - acceptedThisTick.get(side);
        return Math.min(offeredMicroJoules, Math.min(capacityRemaining, sideRemaining));
    }

    private void commit(PipeSide side, long acceptedMicroJoules) {
        if (acceptedMicroJoules == 0) {
            return;
        }
        stored += acceptedMicroJoules;
        acceptedThisTick.put(side, acceptedThisTick.get(side) + acceptedMicroJoules);
    }

    private static PipeSide requireSide(PipeSide side) {
        return Objects.requireNonNull(side, "side");
    }

    private static long validateExternalAmount(long amount, long maximum, String operation) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException(operation + " returned " + amount + " outside [0, " + maximum + "]");
        }
        return amount;
    }

    private static int validateExternalAmount(int amount, int maximum, String operation) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException(operation + " returned " + amount + " outside [0, " + maximum + "]");
        }
        return amount;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
    }
}
