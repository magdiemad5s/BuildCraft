package buildcraft.neo.forge1211.factory;

import buildcraft.neo.forge1211.LegacyModuleIds;

/** Compatibility-facing identifiers and values for the Factory Tank vertical slice. */
public final class FactoryTankContract {
    public static final String MODULE_ID = LegacyModuleIds.FACTORY;
    public static final String REGISTRY_PATH = "tank";
    public static final String FLUID_NBT_KEY = "tank";
    public static final int CAPACITY_MILLIBUCKETS = 16_000;

    private FactoryTankContract() {
    }
}
