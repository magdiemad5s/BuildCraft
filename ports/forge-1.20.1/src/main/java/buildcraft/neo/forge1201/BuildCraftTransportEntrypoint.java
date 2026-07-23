package buildcraft.neo.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegacyModuleIds.TRANSPORT)
public final class BuildCraftTransportEntrypoint extends BuildCraftModuleEntrypoint {
    public BuildCraftTransportEntrypoint(FMLJavaModLoadingContext context) {
        super(LegacyModuleIds.TRANSPORT, context);
    }
}
