package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.SILICON)
public final class BuildCraftSiliconEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftSiliconEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.SILICON, context);
    }
}
