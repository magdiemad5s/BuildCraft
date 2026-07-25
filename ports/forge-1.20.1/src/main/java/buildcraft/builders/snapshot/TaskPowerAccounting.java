/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.snapshot;

/**
 * Exact accounting helpers for an in-flight Builder task.
 *
 * <p>Task animation/work progress is deliberately faster than the MJ removed from the battery. Keeping those two
 * values separate is required: refunding progress would otherwise create energy whenever a task is cancelled.</p>
 */
final class TaskPowerAccounting {
    private TaskPowerAccounting() {
    }

    static long requestedPayment(long remainingProgress, long maximumPayment, int efficiencyMultiplier) {
        if (remainingProgress <= 0 || maximumPayment <= 0 || efficiencyMultiplier <= 0) {
            return 0;
        }
        return Math.min(ceilDiv(remainingProgress, efficiencyMultiplier), maximumPayment);
    }

    static long progressFromPayment(long remainingProgress, long paidPower, int efficiencyMultiplier) {
        if (remainingProgress <= 0 || paidPower <= 0 || efficiencyMultiplier <= 0) {
            return 0;
        }
        long accelerated = paidPower > Long.MAX_VALUE / efficiencyMultiplier
            ? Long.MAX_VALUE
            : paidPower * efficiencyMultiplier;
        return Math.min(remainingProgress, accelerated);
    }

    static long addPaidPower(long currentPaidPower, long payment) {
        if (currentPaidPower <= 0) {
            return Math.max(0, payment);
        }
        if (payment <= 0) {
            return currentPaidPower;
        }
        return currentPaidPower > Long.MAX_VALUE - payment ? Long.MAX_VALUE : currentPaidPower + payment;
    }

    private static long ceilDiv(long value, long divisor) {
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }
}
