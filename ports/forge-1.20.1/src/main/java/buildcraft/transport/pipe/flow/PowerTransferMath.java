/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */

package buildcraft.transport.pipe.flow;

import java.math.BigInteger;

import buildcraft.api.mj.MjAPI;
import buildcraft.transport.BCTransportConfig.PowerLossMode;

/**
 * Overflow-safe arithmetic shared by the MJ pipe network.
 *
 * <p>Loss is charged per hop and only for energy the destination actually
 * accepts. The returned net budget always leaves enough of the gross budget
 * to account for that loss.</p>
 */
public final class PowerTransferMath {
    private PowerTransferMath() {
    }

    public static long saturatingAdd(long left, long right) {
        if (left <= 0) {
            return Math.max(0, right);
        }
        if (right <= 0) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public static long multiplyDivideFloor(long value, long multiplier, long divisor) {
        if (value <= 0 || multiplier <= 0) {
            return 0;
        }
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive");
        }

        long quotient = value / divisor;
        long remainder = value % divisor;
        long high = quotient > Long.MAX_VALUE / multiplier
            ? Long.MAX_VALUE
            : quotient * multiplier;
        long low = remainder <= Long.MAX_VALUE / multiplier
            ? remainder * multiplier / divisor
            : BigInteger.valueOf(remainder)
                .multiply(BigInteger.valueOf(multiplier))
                .divide(BigInteger.valueOf(divisor))
                .longValueExact();
        return saturatingAdd(high, low);
    }

    public static long netBudget(
        long grossBudget, PowerLossMode mode, long absoluteLoss, long resistance
    ) {
        return netBudget(grossBudget, mode, absoluteLoss, resistance, MjAPI.MJ);
    }

    static long netBudget(
        long grossBudget, PowerLossMode mode, long absoluteLoss, long resistance, long microJoulesPerMj
    ) {
        if (grossBudget <= 0) {
            return 0;
        }
        return switch (mode) {
            case LOSSLESS -> grossBudget;
            case PERCENTAGE -> multiplyDivideFloor(
                grossBudget,
                microJoulesPerMj,
                saturatingAdd(microJoulesPerMj, Math.max(0, resistance))
            );
            case ABSOLUTE -> {
                long loss = Math.max(0, absoluteLoss);
                if (loss == 0) {
                    yield grossBudget;
                }
                long doubledLoss = saturatingAdd(loss, loss);
                yield grossBudget <= doubledLoss ? grossBudget / 2 : grossBudget - loss;
            }
        };
    }

    public static long lossForAccepted(
        long accepted, PowerLossMode mode, long absoluteLoss, long resistance
    ) {
        return lossForAccepted(accepted, mode, absoluteLoss, resistance, MjAPI.MJ);
    }

    static long lossForAccepted(
        long accepted, PowerLossMode mode, long absoluteLoss, long resistance, long microJoulesPerMj
    ) {
        if (accepted <= 0) {
            return 0;
        }
        return switch (mode) {
            case LOSSLESS -> 0;
            case PERCENTAGE -> multiplyDivideFloor(
                accepted, Math.max(0, resistance), microJoulesPerMj
            );
            case ABSOLUTE -> Math.min(accepted, Math.max(0, absoluteLoss));
        };
    }

    public static long consumedForAccepted(
        long accepted, PowerLossMode mode, long absoluteLoss, long resistance
    ) {
        return consumedForAccepted(accepted, mode, absoluteLoss, resistance, MjAPI.MJ);
    }

    static long consumedForAccepted(
        long accepted, PowerLossMode mode, long absoluteLoss, long resistance, long microJoulesPerMj
    ) {
        return saturatingAdd(
            Math.max(0, accepted),
            lossForAccepted(accepted, mode, absoluteLoss, resistance, microJoulesPerMj)
        );
    }
}
