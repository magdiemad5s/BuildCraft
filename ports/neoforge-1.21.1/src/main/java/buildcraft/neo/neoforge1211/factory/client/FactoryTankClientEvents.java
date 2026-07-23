// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory.client;

import buildcraft.neo.neoforge1211.LegacyModuleIds;
import buildcraft.neo.neoforge1211.factory.FactoryTankRegistries;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only screen and translucent block-layer registration. */
@EventBusSubscriber(
    modid = LegacyModuleIds.FACTORY,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class FactoryTankClientEvents {
    private FactoryTankClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FactoryTankRegistries.TANK_MENU.get(), TankScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderLayer(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
            FactoryTankRegistries.TANK_BLOCK.get(),
            RenderType.translucent()
        ));
    }
}
