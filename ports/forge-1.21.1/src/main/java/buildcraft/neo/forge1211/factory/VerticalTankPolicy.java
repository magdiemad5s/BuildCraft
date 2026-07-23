package buildcraft.neo.forge1211.factory;

/** Pure ordering and redstone rules shared by the tank block entity. */
public final class VerticalTankPolicy {
    private VerticalTankPolicy() {
    }

    /**
     * Returns indices in the order a fluid should be deposited or withdrawn.
     * Liquids settle at the bottom and gases rise to the top.
     */
    public static int[] traversal(int tankCount, boolean gaseous, boolean filling) {
        if (tankCount < 0) {
            throw new IllegalArgumentException("tankCount must not be negative");
        }
        int[] result = new int[tankCount];
        boolean ascending = filling ? !gaseous : gaseous;
        for (int index = 0; index < tankCount; index++) {
            result[index] = ascending ? index : tankCount - 1 - index;
        }
        return result;
    }

    /** Matches legacy per-block comparator behaviour: empty is 0, nonempty is 1..15. */
    public static int comparatorLevel(int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        int clampedAmount = Math.min(amount, capacity);
        return Math.min(15, (clampedAmount * 14) / capacity + 1);
    }
}
