/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.Date;
import java.util.UUID;

import buildcraft.api.enums.EnumSnapshotType;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersGuis;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.SchematicBlockManager;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.tile.TileReplacer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class ReplacerGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private ReplacerGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void replacerRegistryBlockEntityAndMenuKeepTheirLegacyContract(GameTestHelper helper) {
        assertRegistryId(
            helper,
            ForgeRegistries.BLOCKS.getKey(BCBuildersBlocks.REPLACER.get()),
            "buildcraftbuilders:replacer"
        );
        assertRegistryId(
            helper,
            ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(BCBuildersBlocks.REPLACER_TILE_BC8.get()),
            "buildcraftbuilders:replacer"
        );
        assertRegistryId(
            helper,
            ForgeRegistries.MENU_TYPES.getKey(BCBuildersGuis.MENU_REPLACER.get()),
            "buildcraftbuilders:replacer_menu"
        );

        BlockPos replacerPos = new BlockPos(2, 1, 2);
        helper.setBlock(replacerPos, BCBuildersBlocks.REPLACER.get());
        helper.runAfterDelay(2, () -> {
            BlockEntity blockEntity = helper.getBlockEntity(replacerPos);
            if (!(blockEntity instanceof TileReplacer)) {
                helper.fail("Replacer block did not create TileReplacer: " + blockEntity);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void replacerSlotsFilterLimitAndRoundTripTheirLegacyNbtKeys(GameTestHelper helper) {
        BlockPos replacerPos = new BlockPos(1, 1, 1);
        BlockPos restoredPos = new BlockPos(3, 1, 1);
        BlockPos sourceBlockPos = new BlockPos(1, 1, 3);
        helper.setBlock(replacerPos, BCBuildersBlocks.REPLACER.get());
        helper.setBlock(restoredPos, BCBuildersBlocks.REPLACER.get());
        helper.setBlock(sourceBlockPos, Blocks.STONE);

        helper.runAfterDelay(2, () -> {
            TileReplacer replacer = getReplacer(helper, replacerPos);
            TileReplacer restored = getReplacer(helper, restoredPos);
            if (replacer == null || restored == null) {
                return;
            }

            ItemStack invalid = new ItemStack(Items.DIRT);
            if (replacer.invSnapshot.isItemValid(0, invalid)
                || replacer.invSchematicFrom.isItemValid(0, invalid)
                || replacer.invSchematicTo.isItemValid(0, invalid)) {
                helper.fail("Replacer accepted a vanilla item in a filtered machine slot");
                return;
            }

            ItemStack blueprint = createUsedBlueprint();
            ItemStack schematic = createUsedSchematic(helper, sourceBlockPos);
            if (!replacer.invSnapshot.isItemValid(0, blueprint)
                || !replacer.invSchematicFrom.isItemValid(0, schematic)
                || !replacer.invSchematicTo.isItemValid(0, schematic)) {
                helper.fail("Replacer rejected a valid used blueprint or single-block schematic");
                return;
            }

            ItemStack blueprintBatch = blueprint.copy();
            blueprintBatch.setCount(4);
            ItemStack remainder = replacer.invSnapshot.insertItem(0, blueprintBatch, false);
            if (replacer.invSnapshot.getStackInSlot(0).getCount() != 1 || remainder.getCount() != 3) {
                helper.fail("Replacer blueprint slot did not enforce its one-item limit");
                return;
            }
            replacer.invSchematicFrom.setStackInSlot(0, schematic.copy());
            replacer.invSchematicTo.setStackInSlot(0, schematic.copy());

            CompoundTag saved = new CompoundTag();
            replacer.saveAdditional(saved);
            CompoundTag items = saved.getCompound("items");
            if (!items.contains("snapshot")
                || !items.contains("schematicFrom")
                || !items.contains("schematicTo")) {
                helper.fail("Replacer changed or omitted its legacy item-handler NBT keys: " + items.getAllKeys());
                return;
            }

            restored.load(saved);
            if (!ItemStack.isSameItemSameTags(blueprint, restored.invSnapshot.getStackInSlot(0))
                || !ItemStack.isSameItemSameTags(schematic, restored.invSchematicFrom.getStackInSlot(0))
                || !ItemStack.isSameItemSameTags(schematic, restored.invSchematicTo.getStackInSlot(0))) {
                helper.fail("Replacer inventories did not survive an NBT round trip");
                return;
            }
            helper.succeed();
        });
    }

    private static TileReplacer getReplacer(GameTestHelper helper, BlockPos pos) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (blockEntity instanceof TileReplacer replacer) {
            return replacer;
        }
        helper.fail("Expected TileReplacer at " + pos + " but found " + blockEntity);
        return null;
    }

    private static ItemStack createUsedBlueprint() {
        Snapshot.Header header = new Snapshot.Header(
            new Snapshot.Key(),
            new UUID(1L, 2L),
            "BuildCraft Neo GameTest",
            new Date(0L),
            "Replacer NBT Contract",
            true,
            true,
            true
        );
        return ItemSnapshot.getUsed(EnumSnapshotType.BLUEPRINT, header);
    }

    private static ItemStack createUsedSchematic(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        ISchematicBlock schematic = SchematicBlockManager.getSchematicBlock(new SchematicBlockContext(
            helper.getLevel(),
            absolutePos,
            absolutePos,
            state,
            state.getBlock()
        ));
        ItemStack stack = new ItemStack(BCBuildersItems.SCHEMATIC_SINGLE.get());
        stack.getOrCreateTag().put(ItemSchematicSingle.NBT_KEY, SchematicBlockManager.writeToNBT(schematic));
        stack.setDamageValue(ItemSchematicSingle.DAMAGE_USED);
        return stack;
    }

    private static void assertRegistryId(GameTestHelper helper, ResourceLocation actual, String expected) {
        ResourceLocation expectedId = new ResourceLocation(expected);
        if (!expectedId.equals(actual)) {
            helper.fail("Expected registry ID " + expectedId + " but found " + actual);
        }
    }
}
