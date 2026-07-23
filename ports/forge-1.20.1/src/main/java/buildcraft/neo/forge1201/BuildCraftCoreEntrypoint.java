package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.CORE)
public final class BuildCraftCoreEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftCoreEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.CORE, context);
    }
}
