// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import buildcraft.neo.neoforge1211.LegacyModuleIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Modern NeoForge registrations for the compatibility-preserving Tank slice. */
public final class FactoryTankRegistries {
    public static final ResourceLocation TANK_ID = ResourceLocation.fromNamespaceAndPath(
        LegacyModuleIds.FACTORY,
        FactoryTankContract.REGISTRY_PATH
    );

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LegacyModuleIds.FACTORY);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LegacyModuleIds.FACTORY);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        LegacyModuleIds.FACTORY
    );
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
        BuiltInRegistries.MENU,
        LegacyModuleIds.FACTORY
    );

    public static final DeferredBlock<FactoryTankBlock> TANK_BLOCK = BLOCKS.registerBlock(
        FactoryTankContract.REGISTRY_PATH,
        FactoryTankBlock::new,
        net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.NONE)
            .sound(net.minecraft.world.level.block.SoundType.GLASS)
            .strength(6.0F, 10.0F)
            .noOcclusion()
            .requiresCorrectToolForDrops()
    );
    public static final DeferredItem<BlockItem> TANK_ITEM = ITEMS.register(
        FactoryTankContract.REGISTRY_PATH,
        () -> new BlockItem(TANK_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryTankBlockEntity>> TANK_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            FactoryTankContract.REGISTRY_PATH,
            () -> BlockEntityType.Builder.of(FactoryTankBlockEntity::new, TANK_BLOCK.get()).build(null)
        );
    public static final DeferredHolder<MenuType<?>, MenuType<TankMenu>> TANK_MENU = MENU_TYPES.register(
        FactoryTankContract.REGISTRY_PATH,
        () -> IMenuTypeExtension.create(TankMenu::fromNetwork)
    );

    private FactoryTankRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        modEventBus.addListener(FactoryTankRegistries::addToCreativeTabs);
        modEventBus.addListener(FactoryTankRegistries::registerCapabilities);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(TANK_ITEM);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            TANK_BLOCK_ENTITY.get(),
            (tank, side) -> tank.fluidHandler()
        );
    }
}
