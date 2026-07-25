package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MjTransferAccountingTest {
    @Test
    void convertsReceiverExcessToAcceptedPower() {
        assertEquals(70, MjTransferAccounting.accepted(100, 30));
        assertEquals(100, MjTransferAccounting.accepted(100, 0));
        assertEquals(0, MjTransferAccounting.accepted(100, 100));
    }

    @Test
    void malformedExcessCannotCreateOrOverDrainPower() {
        assertEquals(100, MjTransferAccounting.accepted(100, -1));
        assertEquals(0, MjTransferAccounting.accepted(100, 101));
        assertEquals(0, MjTransferAccounting.accepted(100, Long.MAX_VALUE));
    }

    @Test
    void nonPositiveOffersNeverTransfer() {
        assertEquals(0, MjTransferAccounting.accepted(0, 0));
        assertEquals(0, MjTransferAccounting.accepted(-1, 0));
    }
}
