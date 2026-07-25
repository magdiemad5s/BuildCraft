/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.transport.pipe.flow;

/**
 * Bounded arithmetic shared by transport flows.
 *
 * <p>Pipe event handlers may legitimately remove every offered input or output.
 * Keeping this arithmetic here makes that empty state explicit and prevents
 * NaN/Infinity values from leaking into movement calculations.</p>
 */
public final class FlowRateMath {
    private FlowRateMath() {
    }

    public static double equalShareFactor(int flowRate, int participantCount, int available) {
        if (flowRate <= 0 || participantCount <= 0 || available <= 0) {
            return 0;
        }
        long totalCapacity = (long) flowRate * participantCount;
        long movable = Math.min(totalCapacity, (long) available);
        return movable / (double) flowRate / participantCount;
    }

    public static int clampOffersAndCountPositive(int[] offered, int[] maximums) {
        if (offered.length != maximums.length) {
            throw new IllegalArgumentException("Offer and maximum arrays must have the same length");
        }
        int positive = 0;
        for (int index = 0; index < offered.length; index++) {
            offered[index] = Math.max(0, Math.min(offered[index], maximums[index]));
            if (offered[index] > 0) {
                positive++;
            }
        }
        return positive;
    }
}
