package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.ROBOTICS)
public final class BuildCraftRoboticsEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftRoboticsEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.ROBOTICS, context);
    }
}
