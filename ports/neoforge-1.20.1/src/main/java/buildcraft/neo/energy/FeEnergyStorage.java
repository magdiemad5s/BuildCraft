package buildcraft.neo.energy;

/** Minimal loader-neutral extraction surface for a Forge Energy adapter. */
@FunctionalInterface
public interface FeEnergyStorage {
    int extractEnergy(int maximum, boolean simulate);
}
