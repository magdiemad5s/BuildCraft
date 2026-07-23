package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.TRANSPORT)
public final class BuildCraftTransportEntrypoint {
    public BuildCraftTransportEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.TRANSPORT);
    }
}
