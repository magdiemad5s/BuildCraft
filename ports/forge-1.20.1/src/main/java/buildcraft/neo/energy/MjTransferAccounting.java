package buildcraft.neo.energy;

/** Validates the legacy MJ receiver convention where a receiver returns unaccepted excess power. */
public final class MjTransferAccounting {
    private MjTransferAccounting() {
    }

    /**
     * Returns the accepted part of an offer while containing malformed receiver responses.
     * A negative excess cannot create power, and an excess above the offer cannot drain extra power.
     */
    public static long accepted(long offered, long returnedExcess) {
        if (offered <= 0) {
            return 0;
        }
        long boundedExcess = returnedExcess;
        if (boundedExcess < 0) {
            boundedExcess = 0;
        } else if (boundedExcess > offered) {
            boundedExcess = offered;
        }
        return offered - boundedExcess;
    }
}
