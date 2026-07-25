package buildcraft.neo.energy;

/**
 * Conservation-safe accounting for an integer Forge Energy push into a
 * micro-MJ receiver.
 *
 * <p>An MJ receiver may accept a fractional FE worth of microjoules. The FE
 * API cannot report fractions, so the caller charges one whole FE and keeps
 * the unused micro-MJ as prepaid credit for the next transfer.</p>
 */
public final class FeToMjReceiveAccounting {
    private FeToMjReceiveAccounting() {
    }

    public static Result account(
        int maximumForgeEnergy,
        long prepaidMicroJoules,
        long acceptedMicroJoules,
        long microJoulesPerForgeEnergy
    ) {
        if (maximumForgeEnergy < 0) {
            throw new IllegalArgumentException("maximumForgeEnergy must not be negative");
        }
        if (prepaidMicroJoules < 0) {
            throw new IllegalArgumentException("prepaidMicroJoules must not be negative");
        }
        if (acceptedMicroJoules < 0) {
            throw new IllegalArgumentException("acceptedMicroJoules must not be negative");
        }
        if (microJoulesPerForgeEnergy <= 0) {
            throw new IllegalArgumentException("microJoulesPerForgeEnergy must be positive");
        }

        long maximumOffer = Math.addExact(
            prepaidMicroJoules,
            Math.multiplyExact((long) maximumForgeEnergy, microJoulesPerForgeEnergy)
        );
        if (acceptedMicroJoules > maximumOffer) {
            throw new IllegalArgumentException(
                "acceptedMicroJoules exceeds the prepaid credit plus offered Forge Energy"
            );
        }

        long chargeableMicroJoules = Math.max(0, acceptedMicroJoules - prepaidMicroJoules);
        long chargedForgeEnergy = chargeableMicroJoules == 0
            ? 0
            : 1 + (chargeableMicroJoules - 1) / microJoulesPerForgeEnergy;
        if (chargedForgeEnergy > maximumForgeEnergy) {
            throw new IllegalStateException("calculated Forge Energy exceeds the offered amount");
        }

        long remainingCredit = prepaidMicroJoules
            + chargedForgeEnergy * microJoulesPerForgeEnergy
            - acceptedMicroJoules;
        return new Result((int) chargedForgeEnergy, remainingCredit);
    }

    /**
     * Legacy saves intended this field to hold less than one FE. Malformed or
     * old values are bounded so they cannot create an unbounded energy offer.
     */
    public static long normalizePrepaidCredit(long savedCredit, long microJoulesPerForgeEnergy) {
        if (microJoulesPerForgeEnergy <= 0) {
            throw new IllegalArgumentException("microJoulesPerForgeEnergy must be positive");
        }
        return Math.max(0, Math.min(savedCredit, microJoulesPerForgeEnergy - 1));
    }

    public record Result(int acceptedForgeEnergy, long remainingPrepaidMicroJoules) {
        public Result {
            if (acceptedForgeEnergy < 0 || remainingPrepaidMicroJoules < 0) {
                throw new IllegalArgumentException("Energy accounting values must not be negative");
            }
        }
    }
}
