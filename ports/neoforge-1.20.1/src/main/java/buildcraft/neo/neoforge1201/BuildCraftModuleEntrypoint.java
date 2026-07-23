package buildcraft.neo.neoforge1201;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * The transitional NeoForge 1.20.1 API retains the Forge-named loading context
 * package. Keeping this in one class makes the later package migration explicit.
 */
abstract class BuildCraftModuleEntrypoint {
    protected BuildCraftModuleEntrypoint(String moduleId) {
        BuildCraftNeo.bootstrap(moduleId, FMLJavaModLoadingContext.get().getModEventBus());
    }
}
