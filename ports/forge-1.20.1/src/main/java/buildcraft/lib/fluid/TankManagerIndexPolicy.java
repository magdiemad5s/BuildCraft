package buildcraft.lib.fluid;

/** Bounds checks for externally supplied Forge fluid-tank indexes. */
public final class TankManagerIndexPolicy {
    private TankManagerIndexPolicy() {
    }

    public static boolean contains(int index, int tankCount) {
        return index >= 0 && index < tankCount;
    }
}