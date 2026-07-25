package buildcraft.core.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ListMenuStatePolicyTest {
    @Test
    void lineButtonAndSlotIndexesAreBounded() {
        assertTrue(ListMenuStatePolicy.isValidButton(1, 2, 2));
        assertFalse(ListMenuStatePolicy.isValidButton(-1, 0, 2));
        assertFalse(ListMenuStatePolicy.isValidButton(2, 0, 2));
        assertFalse(ListMenuStatePolicy.isValidButton(0, 3, 2));

        assertTrue(ListMenuStatePolicy.isValidSlot(0, 8, 2, 9));
        assertFalse(ListMenuStatePolicy.isValidSlot(0, 9, 2, 9));
    }

    @Test
    void labelsArePrintableAndMatchTheGuiLimit() {
        assertEquals("", ListMenuStatePolicy.sanitizeLabel(null));
        assertEquals("BuildCraft 100%", ListMenuStatePolicy.sanitizeLabel("Build\u0000Craft 100%"));
        assertEquals(
            "abcdefghijklmnopqrstuvwxyz012345",
            ListMenuStatePolicy.sanitizeLabel("abcdefghijklmnopqrstuvwxyz0123456789")
        );
    }
}
