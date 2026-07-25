package buildcraft.silicon.plug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import buildcraft.test.MinecraftTestBootstrap;

class PluggablePulsarStateTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void savedCountersCannotRemainNegative() {
        assertEquals(0, PluggablePulsar.nonNegative(Integer.MIN_VALUE));
        assertEquals(0, PluggablePulsar.nonNegative(-1));
        assertEquals(9, PluggablePulsar.nonNegative(9));
    }

    @Test
    void queuedPulseCounterSaturatesInsteadOfOverflowing() {
        assertEquals(1, PluggablePulsar.incrementSaturated(0));
        assertEquals(Integer.MAX_VALUE, PluggablePulsar.incrementSaturated(Integer.MAX_VALUE - 1));
        assertEquals(Integer.MAX_VALUE, PluggablePulsar.incrementSaturated(Integer.MAX_VALUE));
    }
}
