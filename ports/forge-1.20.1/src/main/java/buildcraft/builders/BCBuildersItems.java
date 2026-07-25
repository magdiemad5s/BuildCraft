package buildcraft.builders;

import java.util.ArrayList;
import java.util.List;

import buildcraft.builders.item.ItemConstructionMarker;
import buildcraft.builders.item.ItemFillerPlanner;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.item.ItemSnapshot.EnumItemSnapshotType;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.item.LegacyItemMetadata;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BCBuildersItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BCBuilders.MODID);

    public static final RegistryObject<ItemSnapshot> SNAPSHOT = ITEMS.register(
        "snapshot", () -> new ItemSnapshot(new Item.Properties())
    );
    /** Source-compatible aliases; both point to the one legacy metadata item. */
    @Deprecated public static final RegistryObject<ItemSnapshot> BLUEPRINT = SNAPSHOT;
    @Deprecated public static final RegistryObject<ItemSnapshot> TEMPLATE = SNAPSHOT;

    public static final RegistryObject<ItemSchematicSingle> SCHEMATIC_SINGLE = ITEMS.register(
        "schematic_single", () -> new ItemSchematicSingle(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<ItemFillerPlanner> FILLER_PLANNER = ITEMS.register(
        "filler_planner", () -> new ItemFillerPlanner(new Item.Properties())
    );

    public static final RegistryObject<BlockItem> FILLER_BLOCK_ITEM = ITEMS.register(
        "filler", () -> new BlockItem(BCBuildersBlocks.FILLER.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> BUILDER_BLOCK_ITEM = ITEMS.register(
        "builder", () -> new BlockItem(BCBuildersBlocks.BUILDER.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> ARCHITECT_BLOCK_ITEM = ITEMS.register(
        "architect", () -> new BlockItem(BCBuildersBlocks.ARCHITECT.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> LIBRARY_BLOCK_ITEM = ITEMS.register(
        "library", () -> new BlockItem(BCBuildersBlocks.LIBRARY.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> REPLACER_BLOCK_ITEM = ITEMS.register(
        "replacer", () -> new BlockItem(BCBuildersBlocks.REPLACER.get(), new Item.Properties())
    );
    public static final RegistryObject<ItemConstructionMarker> CONSTRUCTION_MARKER = ITEMS.register(
        "marker_construction", () -> new ItemConstructionMarker(new Item.Properties())
    );
    public static final RegistryObject<BlockItem> FRAME_BLOCK_ITEM = ITEMS.register(
        "frame", () -> new BlockItem(BCBuildersBlocks.FRAME.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockItem> QUARRY_BLOCK_ITEM = ITEMS.register(
        "quarry", () -> new BlockItem(BCBuildersBlocks.QUARRY.get(), new Item.Properties())
    );

    private BCBuildersItems() {
    }

    public static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static List<ItemStack> getCreativeTabItems() {
        List<ItemStack> items = new ArrayList<>();
        add(items, CONSTRUCTION_MARKER);
        if (SNAPSHOT.isPresent()) {
            CreativeTabManager.addItemVariants(SNAPSHOT.get(), items::add);
        }
        add(items, SCHEMATIC_SINGLE);
        add(items, FILLER_PLANNER);
        add(items, FILLER_BLOCK_ITEM);
        add(items, BUILDER_BLOCK_ITEM);
        add(items, ARCHITECT_BLOCK_ITEM);
        add(items, LIBRARY_BLOCK_ITEM);
        add(items, REPLACER_BLOCK_ITEM);
        add(items, FRAME_BLOCK_ITEM);
        add(items, QUARRY_BLOCK_ITEM);
        return items;
    }

    private static void add(List<ItemStack> items, RegistryObject<? extends Item> item) {
        if (item.isPresent()) {
            CreativeTabManager.addItemVariants(item.get(), items::add);
        }
    }

    public static void registerItemProperties() {
        ResourceLocation used = new ResourceLocation(BCBuilders.MODID, "used");
        ItemProperties.register(SNAPSHOT.get(), used, (stack, level, entity, seed) ->
            EnumItemSnapshotType.getFromStack(stack).used ? 1.0F : 0.0F
        );
        ItemProperties.register(SCHEMATIC_SINGLE.get(), used, (stack, level, entity, seed) ->
            ItemSchematicSingle.isUsed(stack) ? 1.0F : 0.0F
        );
        ResourceLocation variant = new ResourceLocation(BCBuilders.MODID, "variant");
        ItemProperties.register(SNAPSHOT.get(), variant, (stack, level, entity, seed) ->
            LegacyItemMetadata.get(stack)
        );
        ResourceLocation recording = new ResourceLocation(BCBuilders.MODID, "recording");
        ItemProperties.register(CONSTRUCTION_MARKER.get(), recording, (stack, level, entity, seed) ->
            ItemConstructionMarker.isRecording(stack) ? 1.0F : 0.0F
        );
    }
}