/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TaskPowerAccountingTest {
    @Test
    void oddRemainingProgressRequestsOnlyTheMjActuallyNeeded() {
        assertEquals(3, TaskPowerAccounting.requestedPayment(5, 100, 2));
        assertEquals(5, TaskPowerAccounting.progressFromPayment(5, 3, 2));
    }

    @Test
    void perTickLimitCapsPaymentWithoutChangingTheEfficiencyRatio() {
        assertEquals(4, TaskPowerAccounting.requestedPayment(100, 4, 2));
        assertEquals(8, TaskPowerAccounting.progressFromPayment(100, 4, 2));
    }

    @Test
    void cancelledTasksCanRefundPaymentInsteadOfAcceleratedProgress() {
        long paid = TaskPowerAccounting.addPaidPower(0, 4);
        long progress = TaskPowerAccounting.progressFromPayment(100, paid, 2);

        assertEquals(4, paid);
        assertEquals(8, progress);
    }

    @Test
    void invalidInputsAndOverflowStayBounded() {
        assertEquals(0, TaskPowerAccounting.requestedPayment(0, 10, 2));
        assertEquals(0, TaskPowerAccounting.progressFromPayment(10, 10, 0));
        assertEquals(Long.MAX_VALUE, TaskPowerAccounting.addPaidPower(Long.MAX_VALUE - 2, 8));
        assertEquals(9, TaskPowerAccounting.progressFromPayment(9, Long.MAX_VALUE, 2));
    }
}
