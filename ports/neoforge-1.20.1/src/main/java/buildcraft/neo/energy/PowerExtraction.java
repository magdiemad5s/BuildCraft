package buildcraft.neo.energy;

import java.util.Objects;

/**
 * Validated extraction adapters shared by powered-pipe implementations.
 *
 * <p>Both paths simulate before mutating and reject providers that violate
 * their advertised transfer bounds. This turns a broken provider into a clear
 * error instead of allowing duplicated, negative, or unbounded energy.</p>
 */
public final class PowerExtraction {
    private PowerExtraction() {
    }

    public static long fromPassiveProvider(PassivePowerProvider provider, long maximum) {
        Objects.requireNonNull(provider, "provider");
        requireNonNegative(maximum, "maximum");
        if (maximum == 0) {
            return 0;
        }

        long simulated = validate(provider.extract(0, maximum, true), maximum, "simulated passive extraction");
        if (simulated == 0) {
            return 0;
        }
        return validate(provider.extract(0, simulated, false), simulated, "passive extraction");
    }

    public static int fromFeStorage(FeEnergyStorage storage, int maximum) {
        Objects.requireNonNull(storage, "storage");
        if (maximum < 0) {
            throw new IllegalArgumentException("maximum must not be negative: " + maximum);
        }
        if (maximum == 0) {
            return 0;
        }

        int simulated = validate(storage.extractEnergy(maximum, true), maximum, "simulated FE extraction");
        if (simulated == 0) {
            return 0;
        }
        return validate(storage.extractEnergy(simulated, false), simulated, "FE extraction");
    }

    private static long validate(long amount, long maximum, String operation) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException(operation + " returned " + amount + " outside [0, " + maximum + "]");
        }
        return amount;
    }

    private static int validate(int amount, int maximum, String operation) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException(operation + " returned " + amount + " outside [0, " + maximum + "]");
        }
        return amount;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
    }
}
