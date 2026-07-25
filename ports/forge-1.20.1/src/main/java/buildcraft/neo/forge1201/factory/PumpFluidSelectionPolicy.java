package buildcraft.neo.forge1201.factory;

/** Pure selection rules that stop a pump from mixing or unexpectedly switching fluids. */
public final class PumpFluidSelectionPolicy {
    private PumpFluidSelectionPolicy() {
    }

    public static boolean acceptsCandidate(boolean selectionLocked, boolean candidateMatchesSelection) {
        return !selectionLocked || candidateMatchesSelection;
    }

    public static boolean mayFallbackToAnyFluid(boolean tankEmpty, boolean lockedFluidFound) {
        return tankEmpty && !lockedFluidFound;
    }
}
