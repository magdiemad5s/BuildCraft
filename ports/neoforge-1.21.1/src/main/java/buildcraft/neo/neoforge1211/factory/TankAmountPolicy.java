// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

/** Defensive bounds for saved tank amounts before they reach a fluid handler. */
public final class TankAmountPolicy {
    private TankAmountPolicy() {
    }

    public static int clampStoredAmount(int storedAmount, int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        return Math.max(0, Math.min(storedAmount, capacity));
    }
}
