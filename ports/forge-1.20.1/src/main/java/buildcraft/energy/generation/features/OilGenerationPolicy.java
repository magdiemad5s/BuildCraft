/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.energy.generation.features;

import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/** Pure, deterministic oil-deposit selection shared by world generation and regression tests. */
final class OilGenerationPolicy {
    enum DepositType {
        LARGE,
        MEDIUM,
        LAKE,
        NONE
    }

    private OilGenerationPolicy() {
    }

    static DepositType select(
        DoubleSupplier random,
        double bonus,
        double largeProbabilityPercent,
        double mediumProbabilityPercent,
        double smallProbabilityPercent
    ) {
        if (!Double.isFinite(bonus) || bonus <= 0) {
            return DepositType.NONE;
        }
        if (roll(random, threshold(largeProbabilityPercent, bonus))) {
            return DepositType.LARGE;
        }
        if (roll(random, threshold(mediumProbabilityPercent, bonus))) {
            return DepositType.MEDIUM;
        }
        if (roll(random, threshold(smallProbabilityPercent, bonus))) {
            return DepositType.LAKE;
        }
        return DepositType.NONE;
    }

    static boolean isAllowedByList(boolean listed, boolean blacklist) {
        return listed != blacklist;
    }

    static boolean isLegacyExcessiveBiomeEnabled(
        String namespace,
        String path,
        boolean desertEnabled,
        boolean oceanEnabled
    ) {
        if (!"minecraft".equals(namespace)) {
            return true;
        }
        if ("desert".equals(path)) {
            return desertEnabled;
        }
        if (path.contains("ocean")) {
            return oceanEnabled;
        }
        return true;
    }

    static double combineGenerationRates(double dataRate, double configuredRate) {
        if (!Double.isFinite(dataRate) || !Double.isFinite(configuredRate)
            || dataRate <= 0 || configuredRate <= 0) {
            return 0;
        }
        double combined = dataRate * configuredRate;
        return Double.isFinite(combined) ? combined : 0;
    }

    /**
     * Layers a Forge percentage over its datapack equivalent without changing either source's defaults.
     * The canonical defaults make a default Forge config transparent to datapack overrides and vice versa.
     */
    static double layerPercentage(double dataPercent, double configuredPercent, double canonicalDefaultPercent) {
        if (!Double.isFinite(dataPercent) || !Double.isFinite(configuredPercent)
            || !Double.isFinite(canonicalDefaultPercent)
            || dataPercent <= 0 || configuredPercent <= 0 || canonicalDefaultPercent <= 0) {
            return 0;
        }
        double layered = dataPercent * configuredPercent / canonicalDefaultPercent;
        return Double.isFinite(layered) ? Math.min(100.0, layered) : 0;
    }

    /** Applies an absolute-style Forge height over a datapack value while keeping both default layers transparent. */
    static int layerHeight(int dataHeight, int configuredHeight, int canonicalDefaultHeight) {
        long layered = (long) dataHeight + configuredHeight - canonicalDefaultHeight;
        return (int) Math.max(0, Math.min(256, layered));
    }

    static int chooseInclusiveHeight(IntUnaryOperator nextInt, int firstHeight, int secondHeight) {
        int min = Math.min(firstHeight, secondHeight);
        int max = Math.max(firstHeight, secondHeight);
        if (min == max) {
            return min;
        }
        return min + nextInt.applyAsInt(max - min + 1);
    }

    static int springTubeRadius(DepositType type) {
        return type == DepositType.LARGE ? 1 : 0;
    }

    private static boolean roll(DoubleSupplier random, double thresholdPercent) {
        return thresholdPercent > 0 && random.getAsDouble() * 100.0 < thresholdPercent;
    }

    private static double threshold(double probabilityPercent, double bonus) {
        if (!Double.isFinite(probabilityPercent) || probabilityPercent <= 0) {
            return 0;
        }
        return Math.min(100.0, probabilityPercent * bonus);
    }
}