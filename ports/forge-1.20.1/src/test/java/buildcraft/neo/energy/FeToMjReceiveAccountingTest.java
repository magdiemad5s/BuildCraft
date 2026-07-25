package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FeToMjReceiveAccountingTest {
    @Test
    void fractionalAcceptanceChargesOneFeAndRetainsTheUnusedCredit() {
        var result = FeToMjReceiveAccounting.account(4, 0, 25, 100);

        assertEquals(1, result.acceptedForgeEnergy());
        assertEquals(75, result.remainingPrepaidMicroJoules());
    }

    @Test
    void prepaidCreditIsSpentBeforeAnotherFeIsCharged() {
        var creditOnly = FeToMjReceiveAccounting.account(2, 75, 25, 100);
        assertEquals(0, creditOnly.acceptedForgeEnergy());
        assertEquals(50, creditOnly.remainingPrepaidMicroJoules());

        var oneMoreFe = FeToMjReceiveAccounting.account(2, 75, 175, 100);
        assertEquals(1, oneMoreFe.acceptedForgeEnergy());
        assertEquals(0, oneMoreFe.remainingPrepaidMicroJoules());
    }

    @Test
    void fullOfferRoundTripsWithoutCreatingCredit() {
        var result = FeToMjReceiveAccounting.account(3, 0, 300, 100);

        assertEquals(3, result.acceptedForgeEnergy());
        assertEquals(0, result.remainingPrepaidMicroJoules());
    }

    @Test
    void malformedAcceptanceCannotExceedTheOffer() {
        assertThrows(
            IllegalArgumentException.class,
            () -> FeToMjReceiveAccounting.account(1, 0, 101, 100)
        );
        assertEquals(99, FeToMjReceiveAccounting.normalizePrepaidCredit(Long.MAX_VALUE, 100));
        assertEquals(0, FeToMjReceiveAccounting.normalizePrepaidCredit(-1, 100));
    }
}
