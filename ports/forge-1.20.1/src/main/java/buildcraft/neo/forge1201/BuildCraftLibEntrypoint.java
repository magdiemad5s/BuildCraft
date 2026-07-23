package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.LIB)
public final class BuildCraftLibEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftLibEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.LIB, context);
    }
}
