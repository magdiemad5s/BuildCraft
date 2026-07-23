package buildcraft.neo.forge1211;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/** Shared side-neutral bootstrap for the eight preserved BuildCraft module IDs. */
public final class BuildCraftNeo {
    public static final String RESOURCE_NAMESPACE = "buildcraft";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> INITIALIZED_MODULES = ConcurrentHashMap.newKeySet();

    private BuildCraftNeo() {
    }

    public static void bootstrap(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");
        if (!LegacyModuleIds.isKnown(moduleId)) {
            throw new IllegalArgumentException("Unknown legacy BuildCraft module: " + moduleId);
        }
        if (INITIALIZED_MODULES.add(moduleId)) {
            LOGGER.info("Bootstrapping BuildCraft Neo Better Forge 1.21.1 module {}", moduleId);
        }
    }
}
