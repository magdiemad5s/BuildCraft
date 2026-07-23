package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.CORE)
public final class BuildCraftCoreEntrypoint {
    public BuildCraftCoreEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.CORE);
    }
}
