package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HeatExchangeMenuStatePolicyTest {
    @Test
    void fourFiveFieldTankRecordsUseTheReleasedOffsets() {
        assertEquals(20, HeatExchangeMenuStatePolicy.CLIENT_DATA_COUNT);
        assertEquals(0, HeatExchangeMenuStatePolicy.dataOffsetForTank(0));
        assertEquals(5, HeatExchangeMenuStatePolicy.dataOffsetForTank(1));
        assertEquals(10, HeatExchangeMenuStatePolicy.dataOffsetForTank(2));
        assertEquals(15, HeatExchangeMenuStatePolicy.dataOffsetForTank(3));
    }

    @Test
    void componentButtonOffsetsMapToTheCorrectTank() {
        assertEquals(0, HeatExchangeMenuStatePolicy.tankIndexForButton(0));
        assertEquals(1, HeatExchangeMenuStatePolicy.tankIndexForButton(5));
        assertEquals(2, HeatExchangeMenuStatePolicy.tankIndexForButton(10));
        assertEquals(3, HeatExchangeMenuStatePolicy.tankIndexForButton(15));
    }

    @Test
    void malformedButtonIdsCannotAddressATank() {
        assertEquals(-1, HeatExchangeMenuStatePolicy.tankIndexForButton(-1));
        assertEquals(-1, HeatExchangeMenuStatePolicy.tankIndexForButton(1));
        assertEquals(-1, HeatExchangeMenuStatePolicy.tankIndexForButton(19));
        assertEquals(-1, HeatExchangeMenuStatePolicy.tankIndexForButton(20));
        assertThrows(
            IllegalArgumentException.class,
            () -> HeatExchangeMenuStatePolicy.dataOffsetForTank(4)
        );
    }
}
