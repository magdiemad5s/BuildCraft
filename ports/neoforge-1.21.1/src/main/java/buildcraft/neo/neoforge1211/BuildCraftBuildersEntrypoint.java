package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.BUILDERS)
public final class BuildCraftBuildersEntrypoint {
    public BuildCraftBuildersEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.BUILDERS);
    }
}
