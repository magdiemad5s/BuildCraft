package buildcraft.energy;

import java.util.List;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.blocks.BlockDynamoMJ;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.energy.tile.TileEngineStone_BC8;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.item.MultiBlockItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BCEnergyBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, BCEnergy.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITYS =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BCEnergy.MODID);

    /** Source-compatible aliases to the one canonical legacy buildcraftcore:engine item. */
    @Deprecated
    public static final RegistryObject<MultiBlockItem<EnumEngineType>> ENGINE_STONE_ITEM = BCCoreItems.ENGINE;
    @Deprecated
    public static final RegistryObject<MultiBlockItem<EnumEngineType>> ENGINE_IRON_ITEM = BCCoreItems.ENGINE;

    public static final RegistryObject<BlockDynamoMJ> MJ_DYNAMO = BLOCKS.register(
        "mj_dynamo",
        () -> new BlockDynamoMJ(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape()
        )
    );
    public static final RegistryObject<BlockItem> MJ_DYNAMO_ITEM = BCEnergy.ITEMS.register(
        "mj_dynamo",
        () -> new BlockItem(MJ_DYNAMO.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<TileDynamoMJ>> DYNAMO_MJ_TILE = BLOCK_ENTITYS.register(
        "mj_dynamo",
        () -> BlockEntityType.Builder.of(TileDynamoMJ::new, MJ_DYNAMO.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<TileEngineStone_BC8>> ENGINE_STONE_TILE_BC8 =
        BLOCK_ENTITYS.register(
            "engine.stone",
            () -> BlockEntityType.Builder.of(TileEngineStone_BC8::new, BCCoreBlocks.ENGINE_BC8.get()).build(null)
        );
    public static final RegistryObject<BlockEntityType<TileEngineIron_BC8>> ENGINE_IRON_TILE_BC8 =
        BLOCK_ENTITYS.register(
            "engine.iron",
            () -> BlockEntityType.Builder.of(TileEngineIron_BC8::new, BCCoreBlocks.ENGINE_BC8.get()).build(null)
        );
    public static final RegistryObject<BlockEntityType<TileEngineRF>> ENGINE_RF_TILE = BLOCK_ENTITYS.register(
        "engine.rf",
        () -> BlockEntityType.Builder.of(TileEngineRF::new, BCCoreBlocks.ENGINE_BC8.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<TileSpringOil>> TILE_SPRING = BLOCK_ENTITYS.register(
        "spring.oil",
        () -> BlockEntityType.Builder.of(TileSpringOil::new, BCCoreBlocks.SPRING.get()).build(null)
    );

    static void init(IEventBus bus) {
        BLOCK_ENTITYS.register(bus);
        BLOCKS.register(bus);
    }

    public static List<ItemStack> getCreativeTabItems() {
        return List.of(MJ_DYNAMO_ITEM.get().getDefaultInstance());
    }
}