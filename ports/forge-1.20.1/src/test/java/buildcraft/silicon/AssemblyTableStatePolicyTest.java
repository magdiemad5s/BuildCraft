package buildcraft.silicon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AssemblyTableStatePolicyTest {
    @Test
    void recipeCountRejectsNegativeAndOversizedPackets() {
        assertFalse(AssemblyTableStatePolicy.isValidRecipeCount(-1));
        assertTrue(AssemblyTableStatePolicy.isValidRecipeCount(0));
        assertTrue(AssemblyTableStatePolicy.isValidRecipeCount(AssemblyTableStatePolicy.MAX_SYNCED_RECIPES));
        assertFalse(AssemblyTableStatePolicy.isValidRecipeCount(AssemblyTableStatePolicy.MAX_SYNCED_RECIPES + 1));
    }

    @Test
    void stateOrdinalRejectsMalformedValues() {
        assertNull(AssemblyTableStatePolicy.stateByOrdinal(-1));
        assertEquals(EnumAssemblyRecipeState.POSSIBLE, AssemblyTableStatePolicy.stateByOrdinal(0));
        assertNull(AssemblyTableStatePolicy.stateByOrdinal(EnumAssemblyRecipeState.values().length));
    }

    @Test
    void clientCanOnlyTogglePossibleAndSaved() {
        assertTrue(AssemblyTableStatePolicy.isClientSelectable(EnumAssemblyRecipeState.POSSIBLE));
        assertTrue(AssemblyTableStatePolicy.isClientSelectable(EnumAssemblyRecipeState.SAVED));
        assertFalse(AssemblyTableStatePolicy.isClientSelectable(EnumAssemblyRecipeState.SAVED_ENOUGH));
        assertFalse(AssemblyTableStatePolicy.isClientSelectable(EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE));
    }

    @Test
    void sameSizeRecipeSwapAndStateChangeBothInvalidateTheGuiSnapshot() {
        Map<String, Integer> original = Map.of("buildcraft:first", 0);

        assertTrue(AssemblyTableStatePolicy.hasRecipeStateSnapshotChanged(
            original,
            Map.of("buildcraft:second", 0)
        ));
        assertTrue(AssemblyTableStatePolicy.hasRecipeStateSnapshotChanged(
            original,
            Map.of("buildcraft:first", 1)
        ));
        assertFalse(AssemblyTableStatePolicy.hasRecipeStateSnapshotChanged(
            original,
            Map.of("buildcraft:first", 0)
        ));
    }
}
