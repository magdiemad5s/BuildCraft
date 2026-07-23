package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.SILICON)
public final class BuildCraftSiliconEntrypoint {
    public BuildCraftSiliconEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.SILICON);
    }
}
