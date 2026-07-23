package buildcraft.neo.neoforge1211;

import net.neoforged.fml.common.Mod;

@Mod(LegacyModuleIds.ROBOTICS)
public final class BuildCraftRoboticsEntrypoint {
    public BuildCraftRoboticsEntrypoint() {
        BuildCraftNeo.bootstrap(LegacyModuleIds.ROBOTICS);
    }
}
