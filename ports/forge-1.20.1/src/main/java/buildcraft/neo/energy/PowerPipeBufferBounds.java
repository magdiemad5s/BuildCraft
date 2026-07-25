package buildcraft.neo.energy;

/**
 * Overflow-safe capacity calculations for the two-stage buffer used by an MJ
 * power-pipe section.
 *
 * <p>The active and staged amounts share one capacity. Keeping this logic
 * independent from Minecraft classes makes the simulation and execution paths
 * use exactly the same acceptance rule.</p>
 */
public final class PowerPipeBufferBounds {
    private PowerPipeBufferBounds() {
    }

    /** Calculates how much of an offer fits in the shared capacity. */
    public static long acceptable(long offered, long active, long staged, long capacity) {
        if (offered <= 0 || capacity <= 0 || active < 0 || staged < 0) {
            return 0;
        }
        if (active >= capacity) {
            return 0;
        }

        long remainingAfterActive = capacity - active;
        if (staged >= remainingAfterActive) {
            return 0;
        }
        return Math.min(offered, remainingAfterActive - staged);
    }

    /** Calculates the unaccepted part using the legacy MJ receiver convention. */
    public static long excess(long offered, long active, long staged, long capacity) {
        if (offered <= 0) {
            return offered;
        }
        return offered - acceptable(offered, active, staged, capacity);
    }
}
