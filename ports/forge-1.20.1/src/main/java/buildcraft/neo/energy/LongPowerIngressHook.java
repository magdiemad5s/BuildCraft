package buildcraft.neo.energy;

/**
 * Optional server-thread policy for receiving MJ into a pipe side.
 *
 * <p>This replaces the legacy lossy {@code int} hook return and its
 * {@code -1} sentinel. A hook returns {@link PowerHookResult#useDefaultBehavior()}
 * to defer to the pipe's normal bounds, or a handled result containing the
 * exact accepted amount in long microjoules. The ingress object remains the
 * sole owner of buffer mutation, so a handled hook cannot bypass pipe
 * capacity or per-side throughput limits.</p>
 */
@FunctionalInterface
public interface LongPowerIngressHook {
    /**
     * Evaluates an offered amount before it is committed to the pipe buffer.
     * Implementations must not mutate pipe energy when {@code simulate} is
     * {@code true}.
     */
    PowerHookResult receive(PipeSide side, long offeredMicroJoules, boolean simulate);
}
