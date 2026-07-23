package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.BUILDERS)
public final class BuildCraftBuildersEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftBuildersEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.BUILDERS, context);
    }
}
