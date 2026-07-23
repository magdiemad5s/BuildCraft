package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FeMicroJouleConversionTest {
    @Test
    void convertsWholeFeUnitsExactlyAndLeavesFractionalMjUntouched() {
        FeMicroJouleConversion conversion = new FeMicroJouleConversion(100);

        assertEquals(300, conversion.microJoulesForFe(3));
        assertEquals(3, conversion.feForMicroJoules(399, Integer.MAX_VALUE));
        assertEquals(99, conversion.remainderMicroJoules(399));
        assertEquals(3, conversion.feForMicroJoules(conversion.microJoulesForFe(3), 3));
    }

    @Test
    void subFeMicroJoulesNeverRoundUpToEnergy() {
        FeMicroJouleConversion conversion = new FeMicroJouleConversion(100);

        assertEquals(0, conversion.feForMicroJoules(99, Integer.MAX_VALUE));
        assertEquals(99, conversion.remainderMicroJoules(99));
        assertEquals(1, conversion.feForMicroJoules(100, Integer.MAX_VALUE));
    }

    @Test
    void rejectsConversionFactorsOutsideLegacyConfigurationBounds() {
        assertThrows(IllegalArgumentException.class, () -> new FeMicroJouleConversion(1));
        assertThrows(IllegalArgumentException.class, () -> new FeMicroJouleConversion(99));
        assertThrows(IllegalArgumentException.class, () -> new FeMicroJouleConversion(200_001));
    }
}
