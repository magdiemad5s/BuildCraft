/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

/**
 * Bounds and authority checks shared by the gate menu and its tile-network update path.
 *
 * <p>The numeric container and gate packet IDs remain unchanged. This policy only decides whether an incoming value
 * is legal for the already-open gate.</p>
 */
public final class GateMenuStatePolicy {
    public static final int MAX_VALID_STATEMENTS_PER_KIND = 512;
    public static final int MAX_STATEMENT_ID_LENGTH = 256;

    private GateMenuStatePolicy() {
    }

    public static boolean isValidStatementCount(int count) {
        return count >= 0 && count <= MAX_VALID_STATEMENTS_PER_KIND;
    }

    /**
     * A split eight-slot gate has two independent columns. The hidden connection between the bottom of the first
     * column and the top of the second column is not a selectable connection.
     */
    public static boolean isSelectableConnectionIndex(int slotCount, boolean splitInTwo, int index) {
        if (slotCount <= 1 || index < 0 || index >= slotCount - 1) {
            return false;
        }
        if (!splitInTwo) {
            return true;
        }
        int columnHeight = (slotCount + 1) / 2;
        return index != columnHeight - 1;
    }

    public static boolean canMutateServerGate(
        boolean senderPresent,
        boolean gateMenuOpen,
        boolean exactGate,
        boolean menuStillValid
    ) {
        return senderPresent && gateMenuOpen && exactGate && menuStillValid;
    }

    public static boolean isValidStatementSelection(
        boolean clearing,
        boolean offeredByServer,
        int minimumParameters,
        int supportedParameters,
        boolean parametersCanonical
    ) {
        if (!parametersCanonical) {
            return false;
        }
        if (clearing) {
            return true;
        }
        return offeredByServer
            && minimumParameters >= 0
            && supportedParameters >= 0
            && minimumParameters <= supportedParameters;
    }
}
