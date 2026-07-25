/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.transport.pipe.flow;

/**
 * Invariant-preserving arithmetic for a fluid pipe section's delayed-arrival buckets.
 */
final class FluidSectionAccounting {
    private FluidSectionAccounting() {
    }

    static int maxDrained(int amount, int incomingTotal, int transferPerTick) {
        long movable = (long) amount - Math.max(0, incomingTotal);
        return (int) Math.max(0, Math.min(movable, Math.max(0, transferPerTick)));
    }

    /**
     * Trims delayed arrivals so their total cannot exceed the fluid left in the section.
     *
     * <p>The newest arrivals are removed first, preserving fluid that is closest to completing its travel delay.</p>
     *
     * @return the reconciled total of all delayed-arrival buckets
     */
    static int reconcileIncoming(int amount, int currentTime, int[] incoming) {
        if (incoming.length == 0) {
            return 0;
        }

        long total = 0;
        for (int index = 0; index < incoming.length; index++) {
            incoming[index] = Math.max(0, incoming[index]);
            total += incoming[index];
        }

        long excess = Math.max(0, total - Math.max(0, amount));
        for (int offset = 0; offset < incoming.length && excess > 0; offset++) {
            int index = Math.floorMod(currentTime - offset, incoming.length);
            int removed = (int) Math.min(excess, incoming[index]);
            incoming[index] -= removed;
            excess -= removed;
            total -= removed;
        }
        return (int) total;
    }
}
