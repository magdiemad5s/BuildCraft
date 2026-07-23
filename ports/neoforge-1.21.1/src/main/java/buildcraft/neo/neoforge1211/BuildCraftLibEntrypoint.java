package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.LIB)
public final class BuildCraftLibEntrypoint {
    public BuildCraftLibEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.LIB);
    }
}
