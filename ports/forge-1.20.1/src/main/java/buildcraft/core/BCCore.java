package buildcraft.core;

import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buildcraft.api.BCModules;
import buildcraft.api.enums.EnumSpring;
import buildcraft.api.items.FluidItemDrops;
import buildcraft.core.client.RenderTickListener;
import buildcraft.core.client.model.ModelEngine;
import buildcraft.core.client.render.RenderEngine_BC8;
import buildcraft.core.client.render.RenderMarkerVolume;
import buildcraft.core.client.render.RenderVolumeBoxes;
import buildcraft.core.list.ContainerList;
import buildcraft.core.list.GuiList;
import buildcraft.core.marker.PathCache;
import buildcraft.core.marker.VolumeCache;
import buildcraft.core.marker.volume.MessageVolumeBoxes;
import buildcraft.core.marker.volume.VolumeBox;
import buildcraft.core.marker.volume.WorldSavedDataVolumeBoxes;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.BCLibItems;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.CreativeTabManager.CreativeTabBC;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.gui.BCContainerFactory;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.net.MessageManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(BCCore.MODID)
public class BCCore {
	public static final String MODID = "buildcraftcore";
    public static final CreativeTabBC BUILDCRAFT_TAB = CreativeTabManager.createTab("buildcraft.main");
    public static final CreativeTabBC tabFluids = CreativeTabManager.createTab("buildcraft.fluid");

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "buildcraft");
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.main"))
            .icon(BUILDCRAFT_TAB::makeIcon)
            .displayItems((parameters, output) -> BUILDCRAFT_TAB.accept(List.of(), output::accept))
            .build());
    public static final RegistryObject<CreativeModeTab> FLUID_TAB = CREATIVE_TABS.register("fluid", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.fluid"))
            .icon(tabFluids::makeIcon)
            .displayItems((parameters, output) -> tabFluids.accept(List.of(), output::accept))
            .build());

    public static final Map<String,Object> ENGINE_MAP = new HashMap<>();
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BCCore.MODID);
    public static final RegistryObject<MenuType<ContainerList>> LIST_MENU = MENUS.register(
        "list_menu",
        () -> BCContainerFactory.create(ContainerList::new)
    );
    
    public BCCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::init);
        modEventBus.addListener(this::gatherData);

        BCCoreBlocks.registry(modEventBus);
        BCCoreItems.registry(modEventBus);
        BUILDCRAFT_TAB.addItemProvider(BCCoreItems::getCreativeTabItems);
        BUILDCRAFT_TAB.addItemProvider(BCLibItems::getCreativeTabItems);

        CREATIVE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
//        BCCoreRecipes.init();
        BCCoreConfig.registry(modEventBus);
        ModLoadingContext.get().registerConfig(Type.COMMON, BCCoreConfig.config);
        MessageManager.registerMessageClass(BCModules.CORE, MessageVolumeBoxes.class, MessageVolumeBoxes.HANDLER, MessageVolumeBoxes::toBytes, MessageVolumeBoxes::new, Dist.CLIENT);
        MinecraftForge.EVENT_BUS.register(this);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			eventBus.addListener(RenderTickListener::renderOverlay);
			eventBus.addListener(RenderTickListener::renderLast);
		});
		BCCoreStatements.preInit();
    }

    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeServer(),
            buildcraft.lib.data.NamedDataProvider.of(
                "BuildCraft Core Recipes",
                new BCCoreRecipes(event.getGenerator().getPackOutput())
            )
        );
    }
    
    public void init(final FMLCommonSetupEvent event)
    {
        MarkerCache.registerCache(VolumeCache.INSTANCE);
        MarkerCache.registerCache(PathCache.INSTANCE);
    	EnumSpring.OIL.liquidBlock = BCEnergyFluids.OIL_BLOCK.get(0).get().defaultBlockState();
    	EnumSpring.OIL.tileConstructor = TileSpringOil::new;
    	BCCoreConfig.reloadConfig(MODID);
        BUILDCRAFT_TAB.setItem(BCCoreItems.WRENCH.get());
        FluidItemDrops.item = BCCoreItems.FRAGILE_FLUID_SHARD.get();
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide && event.level.getServer() != null) {
            WorldSavedDataVolumeBoxes.get(event.level).tick();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WorldSavedDataVolumeBoxes volumeBoxes = WorldSavedDataVolumeBoxes.get(player.serverLevel());
        volumeBoxes.volumeBoxes.stream()
            .filter(volumeBox -> volumeBox.isPausedEditingBy(player))
            .forEach(VolumeBox::resumeEditing);

        // Keep the one-tick delay from BuildCraft 8: on an integrated server the
        // play connection is not always ready during PlayerLoggedInEvent itself.
        MessageUtil.doDelayedServer(() -> {
            if (!player.isRemoved() && player.connection != null) {
                MessageManager.sendTo(new MessageVolumeBoxes(volumeBoxes.volumeBoxes), player);
            }
        });
    }
    
    
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
    	public static final ResourceLocation TRUNK_LIGHT = new ResourceLocation("buildcraftcore:blocks/engine/trunk_light");
    	public static final ResourceLocation CHAMBER = new ResourceLocation("buildcraftlib:blocks/engine/chamber_base");
    	public static final ResourceLocation TRUNK = new ResourceLocation("buildcraftcore:blocks/engine/trunk");
    	public static final ResourceLocation ENGINE_MODEL = new ResourceLocation("buildcraftlib:block/engine_base");
        public static final ResourceLocation ENGINE_REDSTONE_TEXTURE_MODEL = new ResourceLocation("buildcraftcore:item/engine_redstone");
        public static final ResourceLocation ENGINE_CREATIVE_TEXTURE_MODEL = new ResourceLocation("buildcraftcore:item/engine_creative");
        public static final ResourceLocation ENGINE_STONE_TEXTURE_MODEL = new ResourceLocation("buildcraftenergy:item/engine_stone");
        public static final ResourceLocation ENGINE_IRON_TEXTURE_MODEL = new ResourceLocation("buildcraftenergy:item/engine_iron");
        public static final ResourceLocation ENGINE_RF_TEXTURE_MODEL = new ResourceLocation("buildcraftenergy:item/engine_rf");
        public static final ResourceLocation ENGINE_LIGHT_SPRITE_MODEL = new ResourceLocation("buildcraftcore:block/engine_trunk_light_sprite");
        public static final ResourceLocation ENGINE_CHAMBER_SPRITE_MODEL = new ResourceLocation("buildcraftlib:block/engine_chamber_sprite");
    	
    	public ClientModEvents() {

		}
    	
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        	BCCoreSprites.init();
        	DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, RenderVolumeBoxes.INSTANCE);
            event.enqueueWork(
                    () -> {
                    	BCCoreItems.registerItemProperties();
                    	MenuScreens.register(LIST_MENU.get(), GuiList::new);
                    }
            );
        }
    	
        @SubscribeEvent
        public static void registryRender(EntityRenderersEvent.RegisterRenderers e) {

        	e.registerBlockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE_BC8.get(), RenderEngine_BC8::new);
        	e.registerBlockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE_BC8.get(), RenderEngine_BC8::new);
        	e.registerBlockEntityRenderer(BCCoreBlocks.MARKER_VOLUME_TILE_BC8.get(), RenderMarkerVolume::new);
        }
        
        @SubscribeEvent
        public static void onModelBakePre(RegisterAdditional event) {
        	event.register(ENGINE_MODEL);
            event.register(ENGINE_REDSTONE_TEXTURE_MODEL);
            event.register(ENGINE_CREATIVE_TEXTURE_MODEL);
            event.register(ENGINE_STONE_TEXTURE_MODEL);
            event.register(ENGINE_IRON_TEXTURE_MODEL);
            event.register(ENGINE_RF_TEXTURE_MODEL);
            event.register(ENGINE_LIGHT_SPRITE_MODEL);
            event.register(ENGINE_CHAMBER_SPRITE_MODEL);
        }
        
        @SubscribeEvent
        public static void onModelBake(ModifyBakingResult event) {
            RenderEngine_BC8.reloadSprites(
                event.getModels().get(ENGINE_LIGHT_SPRITE_MODEL),
                event.getModels().get(ENGINE_CHAMBER_SPRITE_MODEL),
                event.getModels().get(ENGINE_REDSTONE_TEXTURE_MODEL),
                event.getModels().get(ENGINE_CREATIVE_TEXTURE_MODEL),
                event.getModels().get(ENGINE_STONE_TEXTURE_MODEL),
                event.getModels().get(ENGINE_IRON_TEXTURE_MODEL),
                event.getModels().get(ENGINE_RF_TEXTURE_MODEL)
            );
        	ModelEngine.init(event.getModels().get(ENGINE_MODEL));
        	event.getModels().put(new ModelResourceLocation(new ResourceLocation(BCCore.MODID, "engine"), "type=wood"), new ModelEngine(RenderEngine_BC8.REDSTONE_BACK, RenderEngine_BC8.REDSTONE_SIDE));
        	event.getModels().put(new ModelResourceLocation(new ResourceLocation(BCCore.MODID, "engine"), "type=creative"), new ModelEngine(RenderEngine_BC8.CREATIVE_BACK, RenderEngine_BC8.CREATIVE_SIDE));
        	event.getModels().put(new ModelResourceLocation(new ResourceLocation(BCCore.MODID, "engine"), "type=stone"), new ModelEngine(RenderEngine_BC8.STONE_BACK, RenderEngine_BC8.STONE_SIDE));
        	event.getModels().put(new ModelResourceLocation(new ResourceLocation(BCCore.MODID, "engine"), "type=iron"), new ModelEngine(RenderEngine_BC8.IRON_BACK, RenderEngine_BC8.IRON_SIDE));
        	event.getModels().put(new ModelResourceLocation(new ResourceLocation(BCCore.MODID, "engine"), "type=rf"), new ModelEngine(RenderEngine_BC8.RF_BACK, RenderEngine_BC8.RF_SIDE));
        	ModelEngine.release();
        }
        
        @SubscribeEvent
        public static void RegisterItemColor(RegisterColorHandlersEvent.Item event) {
        	event.register(new DynamicFluidContainerModel.Colors(), BCCoreItems.FRAGILE_FLUID_SHARD.get());
        }
        
    }


}

