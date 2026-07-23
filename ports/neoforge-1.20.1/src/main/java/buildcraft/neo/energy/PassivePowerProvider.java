package buildcraft.neo.energy;

/**
 * Loader-neutral form of the legacy MJ passive-provider contract.
 * Implementations return the amount extracted, never the leftover amount.
 */
@FunctionalInterface
public interface PassivePowerProvider {
    long extract(long minimum, long maximum, boolean simulate);
}
