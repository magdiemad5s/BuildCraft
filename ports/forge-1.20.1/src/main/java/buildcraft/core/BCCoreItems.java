package buildcraft.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.annotation.Nullable;

import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumSpring;
import buildcraft.api.items.IMapLocation.MapLocationType;
import buildcraft.core.item.ItemFragileFluidContainer;
import buildcraft.core.item.ItemGoggles;
import buildcraft.core.item.ItemList_BC8;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.item.ItemPaintbrushLegacy;
import buildcraft.core.item.ItemVolumeBox;
import buildcraft.core.item.ItemWrench;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.item.LegacyItemMetadata;
import buildcraft.lib.item.MultiBlockItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BCCoreItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BCCore.MODID);

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", ItemWrench::new);
    public static final RegistryObject<Item> GEAR_WOOD = ITEMS.register("gear_wood", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GEAR_STONE = ITEMS.register("gear_stone", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GEAR_IRON = ITEMS.register("gear_iron", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GEAR_GOLD = ITEMS.register("gear_gold", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GEAR_DIAMOND = ITEMS.register("gear_diamond", () -> new Item(new Item.Properties()));

    public static final EnumMap<DyeColor, ItemPaintbrushLegacy> PAINT_BRUSHS = new EnumMap<>(DyeColor.class);
    public static final RegistryObject<ItemPaintbrushLegacy> PAINT_BRUSH = ITEMS.register("paintbrush", () -> {
        ItemPaintbrushLegacy item = new ItemPaintbrushLegacy(new Item.Properties());
        for (DyeColor colour : DyeColor.values()) {
            PAINT_BRUSHS.put(colour, item);
        }
        return item;
    });

    public static final RegistryObject<ItemMarkerConnector> MARKER_CONNECTOR = ITEMS.register(
        "marker_connector", () -> new ItemMarkerConnector(new Item.Properties())
    );
    public static final RegistryObject<ItemVolumeBox> VOLUME_BOX = ITEMS.register(
        "volume_box", () -> new ItemVolumeBox(new Item.Properties())
    );
    public static final RegistryObject<ItemMapLocation> MAP_LOCATION = ITEMS.register(
        "map_location", () -> new ItemMapLocation(new Item.Properties())
    );
    public static final RegistryObject<ItemList_BC8> LIST = ITEMS.register(
        "list", () -> new ItemList_BC8(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<ItemGoggles> GOGGLES = ITEMS.register(
        "goggles", () -> new ItemGoggles("item.goggles")
    );
    public static final RegistryObject<ItemFragileFluidContainer> FRAGILE_FLUID_SHARD = ITEMS.register(
        "fragile_fluid_shard", ItemFragileFluidContainer::new
    );

    public static final EnumMap<EnumDecoratedBlock, MultiBlockItem<EnumDecoratedBlock>> DECORATED_ITEM_MAP =
        new EnumMap<>(EnumDecoratedBlock.class);
    public static final EnumMap<EnumEngineType, MultiBlockItem<EnumEngineType>> ENGINE_ITEM_MAP =
        new EnumMap<>(EnumEngineType.class);
    public static final EnumMap<EnumSpring, MultiBlockItem<EnumSpring>> SPRING_ITEM_MAP =
        new EnumMap<>(EnumSpring.class);

    public static final RegistryObject<MultiBlockItem<EnumDecoratedBlock>> DECORATED = ITEMS.register(
        "decorated",
        () -> new MultiBlockItem<>(
            BCCoreBlocks.DECORATED.get(),
            new Item.Properties(),
            EnumDecoratedBlock.DESTROY,
            EnumDecoratedBlock.values(),
            DECORATED_ITEM_MAP
        )
    );
    public static final RegistryObject<BlockItem> POWER_TESTER = ITEMS.register(
        "power_tester", () -> new BlockItem(BCCoreBlocks.POWER_TESTER.get(), new Item.Properties())
    );

    public static final RegistryObject<MultiBlockItem<EnumEngineType>> ENGINE = ITEMS.register(
        "engine",
        () -> new MultiBlockItem<>(
            BCCoreBlocks.ENGINE_BC8.get(),
            new Item.Properties(),
            EnumEngineType.WOOD,
            EnumEngineType.values(),
            ENGINE_ITEM_MAP
        )
    );
    public static final RegistryObject<MultiBlockItem<EnumSpring>> SPRING = ITEMS.register(
        "spring",
        () -> new MultiBlockItem<>(
            BCCoreBlocks.SPRING.get(),
            new Item.Properties(),
            EnumSpring.WATER,
            EnumSpring.values(),
            SPRING_ITEM_MAP
        )
    );

    /** Source-compatible aliases; these are not additional registry entries. */
    @Deprecated public static final RegistryObject<MultiBlockItem<EnumEngineType>> ENGINE_RESTONE_ITEM_BC8 = ENGINE;
    @Deprecated public static final RegistryObject<MultiBlockItem<EnumEngineType>> ENGINE_CREATIVE_ITEM_BC8 = ENGINE;
    @Deprecated public static final RegistryObject<MultiBlockItem<EnumSpring>> SPRING_WATER = SPRING;
    @Deprecated public static final RegistryObject<MultiBlockItem<EnumSpring>> SPRING_OIL = SPRING;

    public static final RegistryObject<BlockItem> MARKER_PATH = ITEMS.register(
        "marker_path", () -> new BlockItem(BCCoreBlocks.MARKER_PATH.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> MARKER_VOLUME = ITEMS.register(
        "marker_volume", () -> new BlockItem(BCCoreBlocks.MARKER_VOLUME.get(), new Item.Properties())
    );

    private BCCoreItems() {
    }

    /** Core-owned contents of the BuildCraft main creative tab. */
    public static List<ItemStack> getCreativeTabItems() {
        List<ItemStack> items = new ArrayList<>();
        add(items, MARKER_VOLUME);
        add(items, MARKER_PATH);
        addVariants(items, ENGINE);
        add(items, WRENCH);
        add(items, GEAR_WOOD);
        add(items, GEAR_STONE);
        add(items, GEAR_IRON);
        add(items, GEAR_GOLD);
        add(items, GEAR_DIAMOND);
        addVariants(items, PAINT_BRUSH);
        add(items, LIST);
        add(items, MAP_LOCATION);
        add(items, MARKER_CONNECTOR);
        add(items, VOLUME_BOX);
        add(items, GOGGLES);
        add(items, FRAGILE_FLUID_SHARD);
        addVariants(items, SPRING);
        addVariants(items, DECORATED);
        add(items, POWER_TESTER);
        return items;
    }

    public static ItemStack getDecoratedStack(EnumDecoratedBlock type) {
        return DECORATED.isPresent() ? DECORATED.get().getStack(type) : ItemStack.EMPTY;
    }

    public static ItemStack getEngineStack(EnumEngineType type) {
        return ENGINE.isPresent() ? ENGINE.get().getStack(type) : ItemStack.EMPTY;
    }

    public static ItemStack getSpringStack(EnumSpring type) {
        return SPRING.isPresent() ? SPRING.get().getStack(type) : ItemStack.EMPTY;
    }

    public static ItemStack getPaintbrushStack(@Nullable DyeColor colour) {
        return PAINT_BRUSH.isPresent() ? PAINT_BRUSH.get().getStack(colour) : ItemStack.EMPTY;
    }

    private static void add(List<ItemStack> items, RegistryObject<? extends Item> item) {
        if (item.isPresent()) {
            items.add(item.get().getDefaultInstance());
        }
    }

    private static void addVariants(List<ItemStack> items, RegistryObject<? extends Item> item) {
        if (item.isPresent()) {
            CreativeTabManager.addItemVariants(item.get(), items::add);
        }
    }

    static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void registerItemProperties() {
        ItemProperties.register(
            MAP_LOCATION.get(),
            new ResourceLocation(BCCore.MODID, "map_type"),
            (stack, level, entity, seed) -> (8 - MapLocationType.getFromStack(stack).meta) / 8.0F
        );
        ResourceLocation variant = new ResourceLocation(BCCore.MODID, "variant");
        ItemProperties.register(PAINT_BRUSH.get(), variant, (stack, level, entity, seed) -> LegacyItemMetadata.get(stack));
        ItemProperties.register(DECORATED.get(), variant, (stack, level, entity, seed) -> LegacyItemMetadata.get(stack));
        ItemProperties.register(ENGINE.get(), variant, (stack, level, entity, seed) -> LegacyItemMetadata.get(stack));
        ItemProperties.register(SPRING.get(), variant, (stack, level, entity, seed) -> LegacyItemMetadata.get(stack));
    }
}