package buildcraft.silicon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.lib.item.LegacyItemMetadata;
import buildcraft.silicon.item.ItemGateCopier;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.silicon.item.ItemPluggableLens;
import buildcraft.silicon.item.ItemRedstoneChipsetLegacy;
import buildcraft.silicon.plug.PluggablePulsar;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BCSiliconItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BCSilicon.MODID);

    /** Community-edition addition retained until Robotics' recipe is reconciled separately. */
    public static final RegistryObject<Item> REDSTONE_CRYSTAL = ITEMS.register(
        "redstone_crystal", () -> new Item(new Item.Properties())
    );

    public static final EnumMap<EnumRedstoneChipset, ItemRedstoneChipsetLegacy> REDSTONE_CHIPSET_ITEMS =
        new EnumMap<>(EnumRedstoneChipset.class);
    public static final RegistryObject<ItemRedstoneChipsetLegacy> REDSTONE_CHIPSET = ITEMS.register(
        "redstone_chipset",
        () -> {
            ItemRedstoneChipsetLegacy item = new ItemRedstoneChipsetLegacy(new Item.Properties());
            for (EnumRedstoneChipset type : EnumRedstoneChipset.values()) {
                REDSTONE_CHIPSET_ITEMS.put(type, item);
            }
            return item;
        }
    );

    public static final RegistryObject<ItemPluggableGate> PLUG_GATE_ITEM =
        ITEMS.register("plug_gate", ItemPluggableGate::new);
    public static final RegistryObject<ItemPluggableFacade> PLUG_FACADE_ITEM =
        ITEMS.register("plug_facade", ItemPluggableFacade::new);
    public static final RegistryObject<ItemPluggableLens> PLUG_LENS_ITEM =
        ITEMS.register("plug_lens", ItemPluggableLens::new);
    public static final RegistryObject<ItemPluggableSimple> PLUG_LIGHT_SENSOR_ITEM = ITEMS.register(
        "plug_light_sensor", () -> new ItemPluggableSimple(BCSiliconPlugs.lightSensor, new Item.Properties())
    );
    public static final RegistryObject<ItemPluggableSimple> PLUG_TIMER_ITEM = ITEMS.register(
        "plug_timer", () -> new ItemPluggableSimple(BCSiliconPlugs.timer, new Item.Properties())
    );
    public static final RegistryObject<ItemPluggableSimple> PLUG_PULSAR_ITEM = ITEMS.register(
        "plug_pulsar", () -> new ItemPluggableSimple(
            BCSiliconPlugs.pulsar,
            PluggablePulsar::new,
            ItemPluggableSimple.PIPE_BEHAVIOUR_ACCEPTS_RS_POWER,
            new Item.Properties()
        )
    );
    public static final RegistryObject<ItemGateCopier> GATE_COPIER_ITEM =
        ITEMS.register("gate_copier", ItemGateCopier::new);

    public static final RegistryObject<BlockItem> LASER_BLOCK_ITEM = ITEMS.register(
        "laser", () -> new BlockItem(BCSiliconBlocks.LASER_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> ASSEMBLY_TABLE_ITEM = ITEMS.register(
        "assembly_table", () -> new BlockItem(BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> CHARGING_TABLE_ITEM = ITEMS.register(
        "charging_table", () -> new BlockItem(BCSiliconBlocks.CHARGING_TABLE_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> INTERGRATION_TABLE_ITEM = ITEMS.register(
        "integration_table", () -> new BlockItem(BCSiliconBlocks.INTERGRATION_TABLE_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> ADVANCED_CRAFTING_TABLE_ITEM = ITEMS.register(
        "advanced_crafting_table", () -> new BlockItem(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> PROGRAMMING_TABLE_ITEM = ITEMS.register(
        "programming_table", () -> new BlockItem(BCSiliconBlocks.PROGRAMMING_TABLE_BLOCK.get(), new Item.Properties())
    );

    private BCSiliconItems() {
    }

    public static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static List<ItemStack> getMainTabItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(LASER_BLOCK_ITEM.get().getDefaultInstance());
        items.add(ASSEMBLY_TABLE_ITEM.get().getDefaultInstance());
        items.add(ADVANCED_CRAFTING_TABLE_ITEM.get().getDefaultInstance());
        items.add(INTERGRATION_TABLE_ITEM.get().getDefaultInstance());
        items.add(CHARGING_TABLE_ITEM.get().getDefaultInstance());
        items.add(PROGRAMMING_TABLE_ITEM.get().getDefaultInstance());
        CreativeTabManager.addItemVariants(REDSTONE_CHIPSET.get(), items::add);
        items.add(REDSTONE_CRYSTAL.get().getDefaultInstance());
        items.add(GATE_COPIER_ITEM.get().getDefaultInstance());
        return items;
    }

    public static List<ItemStack> getPlugTabItems() {
        List<ItemStack> items = new ArrayList<>();
        CreativeTabManager.addItemVariants(PLUG_GATE_ITEM.get(), items::add);
        CreativeTabManager.addItemVariants(PLUG_LENS_ITEM.get(), items::add);
        items.add(PLUG_PULSAR_ITEM.get().getDefaultInstance());
        items.add(PLUG_LIGHT_SENSOR_ITEM.get().getDefaultInstance());
        items.add(PLUG_TIMER_ITEM.get().getDefaultInstance());
        return items;
    }

    public static List<ItemStack> getFacadeTabItems() {
        List<ItemStack> items = new ArrayList<>();
        CreativeTabManager.addItemVariants(PLUG_FACADE_ITEM.get(), items::add);
        return items;
    }

    public static void registerItemProperties() {
        ResourceLocation empty = new ResourceLocation(BCSilicon.MODID, "isempty");
        ItemProperties.register(GATE_COPIER_ITEM.get(), empty, (stack, level, entity, seed) ->
            stack.getTag() != null && stack.getTag().contains(ItemGateCopier.NBT_DATA) ? 0.0F : 1.0F
        );
        ResourceLocation variant = new ResourceLocation(BCSilicon.MODID, "variant");
        ItemProperties.register(REDSTONE_CHIPSET.get(), variant, (stack, level, entity, seed) ->
            LegacyItemMetadata.get(stack)
        );
    }
}