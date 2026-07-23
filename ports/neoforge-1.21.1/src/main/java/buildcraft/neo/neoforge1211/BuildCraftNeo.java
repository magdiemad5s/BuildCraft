package buildcraft.neo.neoforge1211;

/**
 * Shared identity boundary for the NeoForge 1.21.1 lane.
 *
 * <p>The release-facing name is BuildCraft Neo Better, while resource and
 * data content keeps the legacy {@value #RESOURCE_NAMESPACE} namespace.</p>
 */
public final class BuildCraftNeo {
    public static final String RESOURCE_NAMESPACE = "buildcraft";

    private BuildCraftNeo() {
    }

    static void bootstrap(String moduleId) {
        if (!LegacyModuleIds.ALL.contains(moduleId)) {
            throw new IllegalArgumentException("Unknown BuildCraft legacy module: " + moduleId);
        }
    }
}
