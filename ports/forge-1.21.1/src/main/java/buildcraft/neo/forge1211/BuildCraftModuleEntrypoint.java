package buildcraft.neo.forge1211;

/** Base constructor shared by the Forge loader entrypoints in this scaffold. */
abstract class BuildCraftModuleEntrypoint {
    protected BuildCraftModuleEntrypoint(String moduleId) {
        BuildCraftNeo.bootstrap(moduleId);
    }
}
