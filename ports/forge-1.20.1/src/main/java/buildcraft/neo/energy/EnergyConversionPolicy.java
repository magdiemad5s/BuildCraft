package buildcraft.neo.energy;

/**
 * Exact, bounded conversion accounting between Forge Energy and BuildCraft micro-MJ.
 *
 * <p>The legacy default is {@code 0.1 MJ = 1 FE}. BuildCraft stores one MJ as one million micro-MJ,
 * therefore the default conversion is 100,000 micro-MJ per FE. Both conversion directions use the
 * same integer rate and round down, so a converter can never create energy from fractional remainders.</p>
 */
public final class EnergyConversionPolicy {
    /** Legacy BuildCraft lower bound: 0.0001 MJ per RF/FE. */
    public static final long MIN_MICRO_MJ_PER_FE = 100L;
    public static final long DEFAULT_MICRO_MJ_PER_FE = 100_000L;
    /** Legacy BuildCraft upper bound: 0.2 MJ per RF/FE. */
    public static final long MAX_MICRO_MJ_PER_FE = 200_000L;
    public static final Transfer NONE = new Transfer(0, 0);

    private EnergyConversionPolicy() {
    }

    /**
     * Calculates a Forge Energy to MJ transfer bounded by input, per-tick throughput, and MJ storage space.
     */
    public static Transfer forgeEnergyToMj(
        int availableForgeEnergy,
        int maxForgeEnergyPerTick,
        long availableMicroMjSpace,
        long microMjPerForgeEnergy
    ) {
        validateRate(microMjPerForgeEnergy);
        if (availableForgeEnergy <= 0 || maxForgeEnergyPerTick <= 0 || availableMicroMjSpace <= 0) {
            return NONE;
        }

        long limitedBySpace = availableMicroMjSpace / microMjPerForgeEnergy;
        long forgeEnergy = Math.min(
            Math.min((long) availableForgeEnergy, (long) maxForgeEnergyPerTick),
            limitedBySpace
        );
        if (forgeEnergy <= 0) {
            return NONE;
        }
        return new Transfer((int) forgeEnergy, forgeEnergy * microMjPerForgeEnergy);
    }

    /**
     * Calculates an MJ to Forge Energy transfer bounded by stored MJ, FE destination space, and throughput.
     */
    public static Transfer mjToForgeEnergy(
        long availableMicroMj,
        int availableForgeEnergySpace,
        int maxForgeEnergyPerTick,
        long microMjPerForgeEnergy
    ) {
        validateRate(microMjPerForgeEnergy);
        if (availableMicroMj <= 0 || availableForgeEnergySpace <= 0 || maxForgeEnergyPerTick <= 0) {
            return NONE;
        }

        long limitedByMj = availableMicroMj / microMjPerForgeEnergy;
        long forgeEnergy = Math.min(
            Math.min((long) availableForgeEnergySpace, (long) maxForgeEnergyPerTick),
            limitedByMj
        );
        if (forgeEnergy <= 0) {
            return NONE;
        }
        return new Transfer((int) forgeEnergy, forgeEnergy * microMjPerForgeEnergy);
    }

    private static void validateRate(long microMjPerForgeEnergy) {
        if (microMjPerForgeEnergy <= 0) {
            throw new IllegalArgumentException("microMjPerForgeEnergy must be positive");
        }
    }

    public record Transfer(int forgeEnergy, long microJoules) {
        public Transfer {
            if (forgeEnergy < 0 || microJoules < 0) {
                throw new IllegalArgumentException("Energy transfers cannot be negative");
            }
        }
    }
}
