package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.FACTORY)
public final class BuildCraftFactoryEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftFactoryEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.FACTORY, context);
    }
}
