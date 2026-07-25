package buildcraft.neo.energy;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Preserves and validates compatibility-sensitive combustion-engine state.
 */
public final class CombustionEngineStatePolicy {
    public static final String RESIDUE_AMOUNT_NBT_KEY = "residueAmount";
    public static final String TANKS_NBT_KEY = "tanks";
    public static final String LEGACY_TANKS_NBT_KEY = "tank";

    private CombustionEngineStatePolicy() {
    }

    public static void writeResidueAmount(CompoundTag nbt, double residueAmount) {
        Objects.requireNonNull(nbt, "nbt");
        nbt.putDouble(RESIDUE_AMOUNT_NBT_KEY, sanitizeResidueAmount(residueAmount));
    }

    public static double readResidueAmount(CompoundTag nbt) {
        Objects.requireNonNull(nbt, "nbt");
        return sanitizeResidueAmount(nbt.getDouble(RESIDUE_AMOUNT_NBT_KEY));
    }

    /**
     * Selects the released BuildCraft tank layout before the early 1.20.1 Community Edition fallback.
     */
    public static CompoundTag selectTankData(CompoundTag nbt) {
        Objects.requireNonNull(nbt, "nbt");
        return nbt.contains(TANKS_NBT_KEY, Tag.TAG_COMPOUND)
            ? nbt.getCompound(TANKS_NBT_KEY)
            : nbt.getCompound(LEGACY_TANKS_NBT_KEY);
    }

    public static double sanitizeResidueAmount(double residueAmount) {
        return Double.isFinite(residueAmount) ? Math.max(0.0, residueAmount) : 0.0;
    }
}