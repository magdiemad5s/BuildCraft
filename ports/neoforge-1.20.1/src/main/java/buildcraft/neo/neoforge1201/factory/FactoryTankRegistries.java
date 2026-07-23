package buildcraft.neo.neoforge1201.factory;

import buildcraft.neo.neoforge1201.LegacyModuleIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Event-based registrations for the Factory Tank.
 *
 * <p>The transitional NeoForge 1.20.1 artifact intentionally retains the
 * Forge-named registry/event packages. This keeps registration in the preserved
 * {@code buildcraftfactory} namespace without changing the legacy checkout.</p>
 */
@Mod.EventBusSubscriber(modid = LegacyModuleIds.FACTORY, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FactoryTankRegistries {
    public static final ResourceLocation TANK_ID = new ResourceLocation(
        LegacyModuleIds.FACTORY,
        FactoryTankContract.REGISTRY_PATH
    );

    private FactoryTankRegistries() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, TANK_ID, FactoryTankBlock::new);
        event.register(
            ForgeRegistries.Keys.ITEMS,
            TANK_ID,
            () -> new BlockItem(tankBlock(), new Item.Properties())
        );
        event.register(
            ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
            TANK_ID,
            () -> BlockEntityType.Builder.of(FactoryTankBlockEntity::new, tankBlock()).build(null)
        );
        event.register(
            ForgeRegistries.Keys.MENU_TYPES,
            TANK_ID,
            () -> IForgeMenuType.create(TankMenu::fromNetwork)
        );
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(tankItem());
        }
    }

    public static FactoryTankBlock tankBlock() {
        Block block = ForgeRegistries.BLOCKS.getValue(TANK_ID);
        if (block instanceof FactoryTankBlock tank) {
            return tank;
        }
        throw new IllegalStateException("Factory Tank block has not been registered");
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<FactoryTankBlockEntity> tankBlockEntityType() {
        BlockEntityType<?> type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(TANK_ID);
        if (type == null) {
            throw new IllegalStateException("Factory Tank block entity has not been registered");
        }
        return (BlockEntityType<FactoryTankBlockEntity>) type;
    }

    @SuppressWarnings("unchecked")
    public static MenuType<TankMenu> tankMenu() {
        MenuType<?> type = ForgeRegistries.MENU_TYPES.getValue(TANK_ID);
        if (type == null) {
            throw new IllegalStateException("Factory Tank menu has not been registered");
        }
        return (MenuType<TankMenu>) type;
    }

    public static BlockItem tankItem() {
        Item item = ForgeRegistries.ITEMS.getValue(TANK_ID);
        if (item instanceof BlockItem blockItem) {
            return blockItem;
        }
        throw new IllegalStateException("Factory Tank item has not been registered");
    }
}
