package buildcraft.neo.forge1201.factory;

/** Bounds persisted and externally supplied Auto Workbench power state. */
public final class AutoWorkbenchStatePolicy {
    private AutoWorkbenchStatePolicy() {
    }

    public static long clampStoredPower(long stored, long capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        return Math.max(0, Math.min(stored, capacity));
    }

    public static long requestedPower(long stored, long capacity) {
        return capacity - clampStoredPower(stored, capacity);
    }

    public static long acceptedPower(long stored, long offered, long capacity) {
        if (offered <= 0) {
            return 0;
        }
        return Math.min(offered, requestedPower(stored, capacity));
    }
}