package buildcraft.neo.forge1201.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistillerMenuStatePolicyTest {
    @Test
    void preservesLegacyIdentityAndNbtKeys() {
        assertEquals("buildcraftfactory", DistillerMenuStatePolicy.MODULE_ID);
        assertEquals("distiller", DistillerMenuStatePolicy.REGISTRY_PATH);
        assertEquals("menu.distiller", DistillerMenuStatePolicy.MENU_REGISTRY_PATH);
        assertEquals("in", DistillerMenuStatePolicy.INPUT_TANK_NBT_KEY);
        assertEquals("gasOut", DistillerMenuStatePolicy.GAS_TANK_NBT_KEY);
        assertEquals("liquidOut", DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY);
        assertEquals("out_gas", DistillerMenuStatePolicy.LEGACY_GAS_TANK_NBT_KEY);
        assertEquals("out_liquid", DistillerMenuStatePolicy.LEGACY_LIQUID_TANK_NBT_KEY);
        assertEquals("battery", DistillerMenuStatePolicy.BATTERY_NBT_KEY);
        assertEquals("mjBattery", DistillerMenuStatePolicy.LEGACY_BATTERY_NBT_KEY);
        assertEquals(4_000, DistillerMenuStatePolicy.TANK_CAPACITY_MILLIBUCKETS);
    }

    @Test
    void tankButtonsUseStableDataOffsets() {
        assertTrue(DistillerMenuStatePolicy.isTankButton(0));
        assertTrue(DistillerMenuStatePolicy.isTankButton(3));
        assertTrue(DistillerMenuStatePolicy.isTankButton(6));
        assertFalse(DistillerMenuStatePolicy.isTankButton(1));
        assertFalse(DistillerMenuStatePolicy.isTankButton(9));
        assertEquals(0, DistillerMenuStatePolicy.tankIndexForButton(0));
        assertEquals(1, DistillerMenuStatePolicy.tankIndexForButton(3));
        assertEquals(2, DistillerMenuStatePolicy.tankIndexForButton(6));
        assertThrows(IllegalArgumentException.class, () ->
            DistillerMenuStatePolicy.tankIndexForButton(4)
        );
    }

    @Test
    void fullWidthTankValuesAreClampedWithoutShortTruncation() {
        assertEquals(80_000, DistillerMenuStatePolicy.clampAmount(80_000, 100_000));
        assertEquals(100_000, DistillerMenuStatePolicy.clampAmount(Integer.MAX_VALUE, 100_000));
        assertEquals(0, DistillerMenuStatePolicy.clampAmount(-1, 100_000));
        assertEquals(0, DistillerMenuStatePolicy.clampAmount(1, -1));
    }
}
