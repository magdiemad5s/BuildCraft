package buildcraft.neo.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MicroJouleBufferTest {
    @Test
    void storesOnlyItsFiniteCapacityAndReturnsExcess() {
        MicroJouleBuffer buffer = new MicroJouleBuffer(100);

        assertEquals(0, buffer.receive(60, false));
        assertEquals(60, buffer.stored());
        assertEquals(40, buffer.receive(80, false));
        assertEquals(100, buffer.stored());
    }

    @Test
    void simulationDoesNotChangeStoredEnergy() {
        MicroJouleBuffer buffer = new MicroJouleBuffer(100);
        buffer.receive(75, false);

        assertEquals(0, buffer.receive(25, true));
        assertEquals(75, buffer.stored());
        assertEquals(50, buffer.extract(0, 50, true));
        assertEquals(75, buffer.stored());
    }

    @Test
    void extractionHonoursMinimumAndMaximum() {
        MicroJouleBuffer buffer = new MicroJouleBuffer(100);
        buffer.receive(30, false);

        assertEquals(0, buffer.extract(40, 50, false));
        assertEquals(30, buffer.stored());
        assertEquals(20, buffer.extract(10, 20, false));
        assertEquals(10, buffer.stored());
    }

    @Test
    void rejectsInvalidAmounts() {
        MicroJouleBuffer buffer = new MicroJouleBuffer(1);
        assertThrows(IllegalArgumentException.class, () -> buffer.receive(-1, false));
        assertThrows(IllegalArgumentException.class, () -> buffer.extract(2, 1, false));
    }
}
