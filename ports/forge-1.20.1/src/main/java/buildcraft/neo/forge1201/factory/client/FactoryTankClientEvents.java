package buildcraft.neo.forge1201.factory.client;

import buildcraft.neo.forge1201.LegacyModuleIds;
import buildcraft.neo.forge1201.factory.FactoryTankRegistries;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only screen and translucent block-layer registration. */
@Mod.EventBusSubscriber(
    modid = LegacyModuleIds.FACTORY,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class FactoryTankClientEvents {
    private FactoryTankClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(FactoryTankRegistries.tankMenu(), TankScreen::new);
            ItemBlockRenderTypes.setRenderLayer(FactoryTankRegistries.tankBlock(), RenderType.cutout());
        });
    }
}
