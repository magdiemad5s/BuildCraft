package buildcraft.lib.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class LegacyTileNbtMigrationTest {
    @Test
    void migratesReleasedRootTankLayoutWithoutLosingFluidData() {
        CompoundTag root = new CompoundTag();
        CompoundTag legacyTank = new CompoundTag();
        legacyTank.putString("FluidName", "oil");
        legacyTank.putInt("Amount", 8_000);
        root.put("tank", legacyTank);

        LegacyTileNbtMigration.migrateLegacyTank(root);

        CompoundTag migrated = root.getCompound("tanks").getCompound("tank");
        assertEquals("oil", migrated.getString("FluidName"));
        assertEquals(8_000, migrated.getInt("Amount"));
    }

    @Test
    void leavesModernTankLayoutUntouchedWhenLegacyTankIsAbsent() {
        CompoundTag root = new CompoundTag();
        CompoundTag modernTanks = new CompoundTag();
        CompoundTag modernTank = new CompoundTag();
        modernTank.putInt("Amount", 4_000);
        modernTanks.put("tank", modernTank);
        root.put("tanks", modernTanks);

        LegacyTileNbtMigration.migrateLegacyTank(root);

        assertEquals(4_000, root.getCompound("tanks").getCompound("tank").getInt("Amount"));
        assertFalse(root.contains("tank"));
    }

    @Test
    void ignoresAnEmptyLegacyTankTag() {
        CompoundTag root = new CompoundTag();
        root.put("tank", new CompoundTag());

        LegacyTileNbtMigration.migrateLegacyTank(root);

        assertTrue(root.getCompound("tanks").isEmpty());
    }

    @Test
    void modernTankLayoutWinsWhenLegacyAndModernLayoutsAreBothPresent() {
        CompoundTag root = new CompoundTag();
        CompoundTag modernTank = new CompoundTag();
        modernTank.putInt("Amount", 4_000);
        CompoundTag modernTanks = new CompoundTag();
        modernTanks.put("tank", modernTank);
        root.put("tanks", modernTanks);

        CompoundTag legacyTank = new CompoundTag();
        legacyTank.putInt("Amount", 8_000);
        root.put("tank", legacyTank);

        LegacyTileNbtMigration.migrateLegacyTank(root);

        assertEquals(4_000, root.getCompound("tanks").getCompound("tank").getInt("Amount"));
    }
}
