package buildcraft.neo.forge1201;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

abstract class BuildCraftModuleEntrypoint {
    protected BuildCraftModuleEntrypoint(String moduleId, FMLJavaModLoadingContext context) {
        BuildCraftNeo.bootstrap(moduleId, context.getModEventBus());
    }
}
