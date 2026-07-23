package buildcraft.neo.energy;

/**
 * A finite, server-thread-confined MJ buffer.
 *
 * <p>{@link #receive(long, boolean)} uses the legacy receiver convention and
 * returns excess energy. {@link #extract(long, long, boolean)} uses the legacy
 * passive-provider convention and returns extracted energy.</p>
 */
public final class MicroJouleBuffer implements PassivePowerProvider {
    private final long capacity;
    private long stored;

    public MicroJouleBuffer(long capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative: " + capacity);
        }
        this.capacity = capacity;
    }

    public long capacity() {
        return capacity;
    }

    public long stored() {
        return stored;
    }

    /** @return energy not accepted by this finite buffer. */
    public long receive(long offered, boolean simulate) {
        if (offered < 0) {
            throw new IllegalArgumentException("offered must not be negative: " + offered);
        }
        long accepted = Math.min(offered, capacity - stored);
        if (!simulate) {
            stored += accepted;
        }
        return offered - accepted;
    }

    @Override
    public long extract(long minimum, long maximum, boolean simulate) {
        if (minimum < 0 || maximum < 0 || minimum > maximum) {
            throw new IllegalArgumentException("invalid extraction range [" + minimum + ", " + maximum + "]");
        }
        long extracted = Math.min(stored, maximum);
        if (extracted < minimum) {
            return 0;
        }
        if (!simulate) {
            stored -= extracted;
        }
        return extracted;
    }
}
