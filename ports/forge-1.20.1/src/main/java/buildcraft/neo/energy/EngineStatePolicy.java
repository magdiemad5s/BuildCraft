package buildcraft.neo.energy;

/** Bounds compatibility-sensitive persisted engine state. */
public final class EngineStatePolicy {
    private EngineStatePolicy() {
    }

    public static long clampStoredPower(long stored, long capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        return Math.max(0, Math.min(stored, capacity));
    }

    /**
     * Adds generated power without allowing negative offers, signed overflow, or storage beyond the engine capacity.
     */
    public static long addStoredPower(long stored, long offered, long capacity) {
        long boundedStored = clampStoredPower(stored, capacity);
        if (offered <= 0 || boundedStored >= capacity) {
            return boundedStored;
        }
        long remaining = capacity - boundedStored;
        return offered >= remaining ? capacity : boundedStored + offered;
    }

    public static double clampHeat(double heat, double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("invalid heat bounds");
        }
        if (!Double.isFinite(heat)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(heat, maximum));
    }

    public static float clampProgress(float progress) {
        if (!Float.isFinite(progress)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(progress, 1.0F));
    }

    public static int clampProgressPart(int progressPart) {
        return Math.max(0, Math.min(progressPart, 2));
    }
}
