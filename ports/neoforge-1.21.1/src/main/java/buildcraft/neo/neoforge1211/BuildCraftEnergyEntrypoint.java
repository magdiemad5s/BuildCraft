package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.ENERGY)
public final class BuildCraftEnergyEntrypoint {
    public BuildCraftEnergyEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.ENERGY);
    }
}
