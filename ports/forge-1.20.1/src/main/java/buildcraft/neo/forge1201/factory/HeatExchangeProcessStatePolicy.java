package buildcraft.neo.forge1201.factory;

/** Packs Heat Exchanger process state and progress into one vanilla menu data value. */
public final class HeatExchangeProcessStatePolicy {
    public static final int STATE_COUNT = 4;
    public static final int MAX_PROGRESS = 120;
    private static final int VALUES_PER_STATE = MAX_PROGRESS + 1;
    public static final int MAX_PACKED_VALUE = STATE_COUNT * VALUES_PER_STATE - 1;

    private HeatExchangeProcessStatePolicy() {
    }

    public static int pack(int stateOrdinal, int progress) {
        if (stateOrdinal < 0 || stateOrdinal >= STATE_COUNT) {
            throw new IllegalArgumentException("Unknown Heat Exchanger process state " + stateOrdinal);
        }
        int safeProgress = Math.max(0, Math.min(progress, MAX_PROGRESS));
        return stateOrdinal * VALUES_PER_STATE + safeProgress;
    }

    public static int stateOrdinal(int packedValue) {
        return sanitize(packedValue) / VALUES_PER_STATE;
    }

    public static int progress(int packedValue) {
        return sanitize(packedValue) % VALUES_PER_STATE;
    }

    public static boolean isActive(int packedValue) {
        return stateOrdinal(packedValue) != 0;
    }

    private static int sanitize(int packedValue) {
        return Math.max(0, Math.min(packedValue, MAX_PACKED_VALUE));
    }
}
