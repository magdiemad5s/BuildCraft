package buildcraft.neo.energy;

/**
 * Bounds persisted and externally reported MJ Dynamo state.
 *
 * <p>Legacy worlds are untrusted input: old or edited NBT must not create negative storage,
 * non-finite animation values, or values above the machine's fixed buffers.</p>
 */
public final class DynamoStatePolicy {
    private DynamoStatePolicy() {
    }

    public static int clampStoredForgeEnergy(int value, int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        return Math.max(0, Math.min(capacity, value));
    }

    public static long clampStoredMicroJoules(long value, long capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        return Math.max(0L, Math.min(capacity, value));
    }

    public static double clampHeat(double value, double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("invalid heat bounds");
        }
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static float clampProgress(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public static int clampProgressPart(int value) {
        return Math.max(0, Math.min(2, value));
    }

    /**
     * Contains a malformed Forge Energy receiver response to the amount that was actually offered.
     */
    public static int acceptedForgeEnergy(int offered, int receiverResponse) {
        if (offered <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(offered, receiverResponse));
    }
}
