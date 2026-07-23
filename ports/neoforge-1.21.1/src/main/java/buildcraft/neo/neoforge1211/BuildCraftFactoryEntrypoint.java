// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211;

import buildcraft.neo.neoforge1211.factory.FactoryTankRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Factory module bootstrap; owns the first gameplay vertical slice. */
@Mod(LegacyModuleIds.FACTORY)
public final class BuildCraftFactoryEntrypoint {
    public BuildCraftFactoryEntrypoint(IEventBus modEventBus) {
        BuildCraftNeo.bootstrap(LegacyModuleIds.FACTORY);
        FactoryTankRegistries.register(modEventBus);
    }
}
