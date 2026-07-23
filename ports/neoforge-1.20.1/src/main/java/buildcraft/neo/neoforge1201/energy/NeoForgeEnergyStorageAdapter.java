package buildcraft.neo.neoforge1201.energy;

import buildcraft.neo.energy.FeEnergyStorage;
import java.util.Objects;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * NeoForge 1.20.1-specific boundary for the bounded pipe-energy core.
 *
 * <p>NeoForge's transitional 47.x API intentionally retains the
 * {@code net.minecraftforge.energy.IEnergyStorage} package. The adapter
 * deliberately exposes only extraction: a future pipe ingress implementation
 * must use {@link buildcraft.neo.energy.BoundedPipeIngress} to apply finite
 * shared capacity, per-side rate limits, and simulation/execute parity before
 * accepting the returned FE.</p>
 */
public final class NeoForgeEnergyStorageAdapter implements FeEnergyStorage {
    private final IEnergyStorage delegate;

    public NeoForgeEnergyStorageAdapter(IEnergyStorage delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int extractEnergy(int maximum, boolean simulate) {
        if (maximum < 0) {
            throw new IllegalArgumentException("maximum must not be negative: " + maximum);
        }
        return delegate.extractEnergy(maximum, simulate);
    }
}
