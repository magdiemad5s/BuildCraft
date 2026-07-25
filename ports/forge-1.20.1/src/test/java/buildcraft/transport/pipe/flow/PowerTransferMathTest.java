package buildcraft.transport.pipe.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import buildcraft.transport.BCTransportConfig.PowerLossMode;

class PowerTransferMathTest {
    private static final long MJ = 1_000_000L;

    @Test
    void losslessModePreservesReleasedTransfer() {
        assertEquals(100, PowerTransferMath.netBudget(100, PowerLossMode.LOSSLESS, 30, MJ, MJ));
        assertEquals(0, PowerTransferMath.lossForAccepted(100, PowerLossMode.LOSSLESS, 30, MJ, MJ));
        assertEquals(100, PowerTransferMath.consumedForAccepted(100, PowerLossMode.LOSSLESS, 30, MJ, MJ));
    }

    @Test
    void percentageModeConservesGrossBudget() {
        long resistance = MJ / 4;
        long net = PowerTransferMath.netBudget(100, PowerLossMode.PERCENTAGE, 0, resistance, MJ);

        assertEquals(80, net);
        assertEquals(20, PowerTransferMath.lossForAccepted(net, PowerLossMode.PERCENTAGE, 0, resistance, MJ));
        assertEquals(100, PowerTransferMath.consumedForAccepted(net, PowerLossMode.PERCENTAGE, 0, resistance, MJ));
    }

    @Test
    void absoluteModeChargesOnlyActuallyAcceptedPower() {
        assertEquals(70, PowerTransferMath.netBudget(100, PowerLossMode.ABSOLUTE, 30, 0, MJ));
        assertEquals(30, PowerTransferMath.lossForAccepted(70, PowerLossMode.ABSOLUTE, 30, 0, MJ));
        assertEquals(100, PowerTransferMath.consumedForAccepted(70, PowerLossMode.ABSOLUTE, 30, 0, MJ));

        assertEquals(10, PowerTransferMath.netBudget(20, PowerLossMode.ABSOLUTE, 30, 0, MJ));
        assertEquals(10, PowerTransferMath.lossForAccepted(10, PowerLossMode.ABSOLUTE, 30, 0, MJ));
        assertEquals(20, PowerTransferMath.consumedForAccepted(10, PowerLossMode.ABSOLUTE, 30, 0, MJ));

        assertEquals(10, PowerTransferMath.consumedForAccepted(5, PowerLossMode.ABSOLUTE, 30, 0, MJ));
    }

    @Test
    void saturationAndRatioMathNeverWrapNegative() {
        assertEquals(Long.MAX_VALUE, PowerTransferMath.saturatingAdd(Long.MAX_VALUE, 1));
        assertEquals(MJ, PowerTransferMath.multiplyDivideFloor(Long.MAX_VALUE, MJ, Long.MAX_VALUE));
        long net = PowerTransferMath.netBudget(
            Long.MAX_VALUE, PowerLossMode.PERCENTAGE, 0, MJ, MJ
        );
        long consumed = PowerTransferMath.consumedForAccepted(
            net, PowerLossMode.PERCENTAGE, 0, MJ, MJ
        );

        assertTrue(net > 0);
        assertTrue(consumed > 0);
        assertTrue(consumed <= Long.MAX_VALUE);
    }
}
