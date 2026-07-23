package buildcraft.neo.neoforge1201;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** Shared bootstrap for legacy BuildCraft module identities in this NeoForge 1.20.1 lane. */
public final class BuildCraftNeo {
    public static final String RESOURCE_NAMESPACE = "buildcraft";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> INITIALIZED_MODULES = ConcurrentHashMap.newKeySet();

    private BuildCraftNeo() {
    }

    public static void bootstrap(String moduleId, IEventBus modEventBus) {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(modEventBus, "modEventBus");
        if (!LegacyModuleIds.isKnown(moduleId)) {
            throw new IllegalArgumentException("Unknown legacy BuildCraft module: " + moduleId);
        }
        if (INITIALIZED_MODULES.add(moduleId)) {
            LOGGER.info("Bootstrapping BuildCraft Neo Better module {}", moduleId);
            modEventBus.addListener((FMLCommonSetupEvent event) ->
                LOGGER.debug("Common setup reached for BuildCraft Neo Better module {}", moduleId)
            );
        }
    }
}
