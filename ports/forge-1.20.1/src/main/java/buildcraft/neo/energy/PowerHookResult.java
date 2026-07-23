package buildcraft.neo.energy;

/**
 * Explicit replacement for the legacy power hook's lossy {@code int} return
 * value and {@code -1} sentinel. Amounts remain long microjoules end to end.
 */
public record PowerHookResult(boolean handled, long accepted) {
    public PowerHookResult {
        if (accepted < 0) {
            throw new IllegalArgumentException("accepted must not be negative: " + accepted);
        }
        if (!handled && accepted != 0) {
            throw new IllegalArgumentException("a default-behavior result cannot accept power");
        }
    }

    public static PowerHookResult useDefaultBehavior() {
        return new PowerHookResult(false, 0);
    }

    public static PowerHookResult handled(long accepted) {
        return new PowerHookResult(true, accepted);
    }

    public long acceptedFrom(long offered) {
        if (!handled) {
            throw new IllegalStateException("default behavior has no hook acceptance value");
        }
        if (offered < 0 || accepted > offered) {
            throw new IllegalArgumentException("accepted energy must be within offered energy");
        }
        return accepted;
    }
}
