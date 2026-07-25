/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.transport.pipe.flow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidSectionAccountingTest {
    @Test
    void maximumDrainIsNonNegativeAndTransferBounded() {
        assertEquals(0, FluidSectionAccounting.maxDrained(20, 40, 100));
        assertEquals(0, FluidSectionAccounting.maxDrained(20, 20, 100));
        assertEquals(30, FluidSectionAccounting.maxDrained(80, 20, 30));
        assertEquals(60, FluidSectionAccounting.maxDrained(80, 20, 100));
        assertEquals(0, FluidSectionAccounting.maxDrained(80, 20, -1));
    }

    @Test
    void forcedDrainTrimsNewestDelayedArrivalsToRemainingAmount() {
        int[] incoming = {40, 30, 20, 10};

        int reconciled = FluidSectionAccounting.reconcileIncoming(45, 2, incoming);

        assertEquals(45, reconciled);
        assertArrayEquals(new int[] {35, 0, 0, 10}, incoming);
    }

    @Test
    void forcedDrainToEmptyClearsEveryDelayedArrival() {
        int[] incoming = {10, 20, 30};

        int reconciled = FluidSectionAccounting.reconcileIncoming(0, 1, incoming);

        assertEquals(0, reconciled);
        assertArrayEquals(new int[] {0, 0, 0}, incoming);
    }

    @Test
    void reconciliationRepairsInvalidNegativeBucketsAndPreservesValidTotals() {
        int[] incoming = {-5, 8, 13};

        int reconciled = FluidSectionAccounting.reconcileIncoming(100, 0, incoming);

        assertEquals(21, reconciled);
        assertArrayEquals(new int[] {0, 8, 13}, incoming);
    }
}
