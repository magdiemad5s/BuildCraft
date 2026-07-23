package buildcraft.neo.energy;

/**
 * Exact integer conversion between Forge Energy units and microjoules.
 *
 * <p>FE is an integer API. Consequently, conversion from microjoules always
 * rounds down and leaves any remainder in the MJ store; it never rounds up
 * into a whole FE unit. This prevents a buffer containing less than one FE's
 * worth of MJ from creating energy during an FE capability query.</p>
 */
public final class FeMicroJouleConversion {
    /** Legacy BuildCraft lower configuration bound. */
    public static final long MIN_MICRO_JOULES_PER_FE = 100L;

    /** Legacy BuildCraft upper configuration bound. */
    public static final long MAX_MICRO_JOULES_PER_FE = 200_000L;

    /** Legacy BuildCraft default: ten FE per Minecraft Joule. */
    public static final long DEFAULT_MICRO_JOULES_PER_FE = 100_000L;

    private final long microJoulesPerFe;

    public FeMicroJouleConversion(long microJoulesPerFe) {
        if (microJoulesPerFe < MIN_MICRO_JOULES_PER_FE || microJoulesPerFe > MAX_MICRO_JOULES_PER_FE) {
            throw new IllegalArgumentException(
                "microJoulesPerFe must be within legacy BuildCraft bounds ["
                    + MIN_MICRO_JOULES_PER_FE + ", " + MAX_MICRO_JOULES_PER_FE + "]: " + microJoulesPerFe
            );
        }
        this.microJoulesPerFe = microJoulesPerFe;
    }

    public static FeMicroJouleConversion legacyDefault() {
        return new FeMicroJouleConversion(DEFAULT_MICRO_JOULES_PER_FE);
    }

    public long microJoulesPerFe() {
        return microJoulesPerFe;
    }

    /** Returns the exact MJ quantity represented by an FE API amount. */
    public long microJoulesForFe(int forgeEnergy) {
        if (forgeEnergy < 0) {
            throw new IllegalArgumentException("forgeEnergy must not be negative: " + forgeEnergy);
        }
        return microJoulesPerFe * forgeEnergy;
    }

    /**
     * Returns the whole FE units available without consuming or rounding up
     * any sub-FE MJ remainder.
     */
    public int feForMicroJoules(long availableMicroJoules, int maximumForgeEnergy) {
        if (availableMicroJoules < 0) {
            throw new IllegalArgumentException("availableMicroJoules must not be negative: " + availableMicroJoules);
        }
        if (maximumForgeEnergy < 0) {
            throw new IllegalArgumentException("maximumForgeEnergy must not be negative: " + maximumForgeEnergy);
        }

        long wholeFe = availableMicroJoules / microJoulesPerFe;
        return (int) Math.min(wholeFe, (long) maximumForgeEnergy);
    }

    /** Returns the MJ that cannot be represented by a whole FE unit yet. */
    public long remainderMicroJoules(long availableMicroJoules) {
        if (availableMicroJoules < 0) {
            throw new IllegalArgumentException("availableMicroJoules must not be negative: " + availableMicroJoules);
        }
        return availableMicroJoules % microJoulesPerFe;
    }
}
