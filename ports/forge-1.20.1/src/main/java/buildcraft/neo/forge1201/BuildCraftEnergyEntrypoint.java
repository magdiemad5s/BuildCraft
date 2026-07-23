package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.ENERGY)
public final class BuildCraftEnergyEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftEnergyEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.ENERGY, context);
    }
}
