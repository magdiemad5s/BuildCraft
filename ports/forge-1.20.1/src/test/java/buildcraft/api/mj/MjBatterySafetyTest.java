/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.api.mj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

class MjBatterySafetyTest {
    @Test
    void negativeCapacityIsRejectedButZeroCapacityRemainsSupported() {
        assertThrows(IllegalArgumentException.class, () -> new MjBattery(-1));
        assertEquals(0, new MjBattery(0).getCapacity());
    }

    @Test
    void checkedAdditionPreservesReleasedOverchargeSemantics() {
        MjBattery battery = new MjBattery(100);
        battery.addPower(90, FluidAction.EXECUTE);

        assertEquals(0, battery.addPowerChecking(50, FluidAction.SIMULATE));
        assertEquals(90, battery.getStored());
        assertEquals(0, battery.addPowerChecking(50, FluidAction.EXECUTE));
        assertEquals(140, battery.getStored());

        assertEquals(1, battery.addPowerChecking(1, FluidAction.SIMULATE));
        assertEquals(1, battery.addPowerChecking(1, FluidAction.EXECUTE));
        assertEquals(140, battery.getStored());
    }

    @Test
    void additionsSaturateAtLongMaxAndSimulationMatchesExecution() {
        MjBattery battery = batteryWithStored(Long.MAX_VALUE, Long.MAX_VALUE - 5);

        assertEquals(5, battery.addPowerChecking(10, FluidAction.SIMULATE));
        assertEquals(Long.MAX_VALUE - 5, battery.getStored());
        assertEquals(5, battery.addPowerChecking(10, FluidAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, battery.getStored());

        assertEquals(10, battery.addPower(10, FluidAction.SIMULATE));
        assertEquals(10, battery.addPower(10, FluidAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, battery.getStored());
    }

    @Test
    void nonPositiveAdditionCannotDrainOrCorruptTheBattery() {
        MjBattery battery = batteryWithStored(100, 75);

        assertEquals(0, battery.addPower(-25, FluidAction.EXECUTE));
        assertEquals(0, battery.addPowerChecking(Long.MIN_VALUE, FluidAction.EXECUTE));
        assertEquals(0, battery.addPower(0, FluidAction.EXECUTE));
        assertEquals(75, battery.getStored());
    }

    @Test
    void invalidExtractionRangesCannotIncreaseStoredPower() {
        MjBattery battery = batteryWithStored(100, 75);

        assertEquals(0, battery.extractPower(-10, -1));
        assertEquals(0, battery.extractPower(-10, 10));
        assertEquals(0, battery.extractPower(20, 10));
        assertFalse(battery.extractPower(-1));
        assertEquals(75, battery.getStored());

        assertEquals(50, battery.extractPower(20, 50));
        assertEquals(25, battery.getStored());
    }

    @Test
    void nbtKeepsTheStoredKeyAndSanitizesOnlyNegativeState() {
        MjBattery battery = new MjBattery(100);
        CompoundTag malformed = new CompoundTag();
        malformed.putLong("stored", Long.MIN_VALUE);
        battery.deserializeNBT(malformed);
        assertEquals(0, battery.getStored());

        CompoundTag overcharged = new CompoundTag();
        overcharged.putLong("stored", 250);
        battery.deserializeNBT(overcharged);
        assertEquals(250, battery.getStored());
        assertEquals(250, battery.serializeNBT().getLong("stored"));

        battery.deserializeNBT(null);
        assertEquals(0, battery.getStored());
    }

    @Test
    void bufferRoundTripPreservesOverchargeAndSanitizesNegativeState() {
        MjBattery battery = batteryWithStored(100, 250);
        ByteBuf encoded = Unpooled.buffer(Long.BYTES);
        ByteBuf malformed = Unpooled.buffer(Long.BYTES);
        try {
            battery.writeToBuffer(encoded);
            MjBattery decoded = new MjBattery(100);
            decoded.readFromBuffer(encoded);
            assertEquals(250, decoded.getStored());

            malformed.writeLong(Long.MIN_VALUE);
            decoded.readFromBuffer(malformed);
            assertEquals(0, decoded.getStored());
        } finally {
            encoded.release();
            malformed.release();
        }
    }

    @Test
    void powerLossThresholdDoesNotOverflowForLargeCapacities() {
        MjBattery battery = batteryWithStored(Long.MAX_VALUE, Long.MAX_VALUE);

        battery.tick(null, Vec3.ZERO);

        assertEquals(Long.MAX_VALUE, battery.getStored());
    }

    @Test
    void ordinaryOverchargeStillBleedsAboveTwiceCapacity() {
        assertEquals(2, MjBattery.calculatePowerLoss(233, 100));
        assertEquals(0, MjBattery.calculatePowerLoss(200, 100));
    }

    private static MjBattery batteryWithStored(long capacity, long stored) {
        MjBattery battery = new MjBattery(capacity);
        CompoundTag tag = new CompoundTag();
        tag.putLong("stored", stored);
        battery.deserializeNBT(tag);
        return battery;
    }
}
