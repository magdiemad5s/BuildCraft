/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;
import java.util.UUID;

import com.mojang.authlib.GameProfile;


import buildcraft.api.lists.ListMatchHandler.Type;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemWrench;
import buildcraft.core.marker.PathCache;
import buildcraft.core.marker.PathConnection;
import buildcraft.core.marker.PathSavedData;
import buildcraft.core.marker.PathSubCache;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.list.ListMatchHandlerOreDictionary;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime regression coverage for BuildCraft's own wrench contract. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class CoreInteractionGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final ResourceLocation WRENCHED_ADVANCEMENT =
        new ResourceLocation("buildcraftcore", "wrenched");

    private CoreInteractionGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void wrenchRotatesBuildCraftBlocksAndAwardsLegacyAdvancement(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(1, 1, 1);
        BlockState initialState = BCBuildersBlocks.BUILDER.get().defaultBlockState()
            .setValue(BlockBCBase_Neptune.PROP_FACING, Direction.NORTH);
        helper.setBlock(localPos, initialState);

        helper.runAfterDelay(2, () -> {
            // Forge deliberately refuses to grant advancements to FakePlayer instances. Use an otherwise detached
            // vanilla server player so this test exercises the same advancement path as an actual connected player.
            ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.fromString("8500bf9a-1458-4a53-bd9a-493917e37fe7"), "BCWrenchTest")
            );
            BlockPos playerPos = helper.absolutePos(localPos);
            player.setPos(
                playerPos.getX() + 0.5,
                playerPos.getY() + 0.5,
                playerPos.getZ() + 0.5
            );
            ItemStack wrenchStack = new ItemStack(BCCoreItems.WRENCH.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, wrenchStack);

            BlockPos absolutePos = helper.absolutePos(localPos);
            BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolutePos),
                Direction.UP,
                absolutePos,
                false
            );
            ItemWrench wrench = (ItemWrench) BCCoreItems.WRENCH.get();
            if (!wrench.canWrench(player, InteractionHand.MAIN_HAND, wrenchStack, hit)) {
                helper.fail("BuildCraft's own wrench rejected its IToolWrench contract");
                return;
            }

            InteractionResult result = wrench.useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
            );
            if (!result.consumesAction()) {
                helper.fail("Wrench did not report a successful BuildCraft block rotation");
                return;
            }

            BlockState rotatedState = helper.getLevel().getBlockState(absolutePos);
            if (rotatedState.getValue(BlockBCBase_Neptune.PROP_FACING) != Direction.WEST) {
                helper.fail("Wrench did not rotate the Builder from north to west");
                return;
            }

            Advancement advancement = helper.getLevel().getServer().getAdvancements()
                .getAdvancement(WRENCHED_ADVANCEMENT);
            if (advancement == null) {
                helper.fail("Legacy buildcraftcore:wrenched advancement did not load");
                return;
            }
            if (!player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                helper.fail("Successful wrench use did not award buildcraftcore:wrenched");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void listMatchingUsesModernForgeTagsForLegacyClassTypeAndMaterialModes(GameTestHelper helper) {
        ListMatchHandlerOreDictionary handler = new ListMatchHandlerOreDictionary();
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);
        ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
        ItemStack ironOre = new ItemStack(Items.IRON_ORE);
        ItemStack deepslateIronOre = new ItemStack(Items.DEEPSLATE_IRON_ORE);

        if (!handler.matches(Type.CLASS, ironOre, deepslateIronOre, true)) {
            helper.fail("List CLASS mode did not match two entries in forge:ores/iron");
            return;
        }
        if (!handler.matches(Type.TYPE, ironIngot, copperIngot, true)) {
            helper.fail("List TYPE mode did not match two Forge ingot tags");
            return;
        }
        if (!handler.matches(Type.MATERIAL, ironIngot, ironOre, true)) {
            helper.fail("List MATERIAL mode did not match iron across ingot and ore tags");
            return;
        }
        if (handler.matches(Type.TYPE, ironIngot, ironOre, false)) {
            helper.fail("List TYPE mode treated ingots and ores as the same type");
            return;
        }
        if (handler.matches(Type.MATERIAL, ironIngot, copperIngot, false)) {
            helper.fail("List MATERIAL mode treated iron and copper as the same material");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void pathMarkerLoopClosureRefreshesAndPersistsItsOrderedConnection(GameTestHelper helper) {
        BlockPos firstLocal = new BlockPos(1, 1, 1);
        BlockPos middleLocal = new BlockPos(3, 1, 1);
        BlockPos lastLocal = new BlockPos(3, 1, 3);
        helper.setBlock(firstLocal, buildcraft.core.BCCoreBlocks.MARKER_PATH.get());
        helper.setBlock(middleLocal, buildcraft.core.BCCoreBlocks.MARKER_PATH.get());
        helper.setBlock(lastLocal, buildcraft.core.BCCoreBlocks.MARKER_PATH.get());

        helper.runAfterDelay(3, () -> {
            BlockPos first = helper.absolutePos(firstLocal);
            BlockPos middle = helper.absolutePos(middleLocal);
            BlockPos last = helper.absolutePos(lastLocal);
            PathSubCache cache = PathCache.INSTANCE.getSubCache(helper.getLevel());
            if (!cache.tryConnect(first, middle) || !cache.tryConnect(middle, last)) {
                helper.fail("Path marker cache did not build a three-marker path");
                return;
            }

            PathConnection connection = cache.getConnection(first);
            if (connection == null || connection != cache.getConnection(last)) {
                helper.fail("Path markers did not resolve to one ordered connection");
                return;
            }
            cache.setDirty(false);
            if (!connection.canAddMarker(last, first) || !connection.addMarker(last, first)) {
                helper.fail("Path connection could not close a loop from its last marker to its first");
                return;
            }
            if (!cache.isDirty()) {
                helper.fail("Closing a path loop did not dirty its SavedData");
                return;
            }
            if (connection.getMarkerPositions().size() != 4
                || !connection.getMarkerPositions().get(0).equals(first)
                || !connection.getMarkerPositions().get(3).equals(first)) {
                helper.fail("Closed path did not retain the legacy repeated-first-marker representation");
                return;
            }

            PathSavedData savedData = new PathSavedData();
            savedData.setCache(cache);
            net.minecraft.nbt.CompoundTag serialized = savedData.save(new net.minecraft.nbt.CompoundTag());
            net.minecraft.nbt.ListTag connections = serialized.getList("connections", net.minecraft.nbt.Tag.TAG_LIST);
            boolean foundSerializedLoop = false;
            for (net.minecraft.nbt.Tag entry : connections) {
                if (!(entry instanceof net.minecraft.nbt.ListTag positions) || positions.size() != 4) {
                    continue;
                }
                BlockPos savedFirst = net.minecraft.nbt.NbtUtils.readBlockPos(
                    (net.minecraft.nbt.CompoundTag) positions.get(0)
                );
                BlockPos savedLast = net.minecraft.nbt.NbtUtils.readBlockPos(
                    (net.minecraft.nbt.CompoundTag) positions.get(positions.size() - 1)
                );
                if (savedFirst.equals(first) && savedLast.equals(first)) {
                    foundSerializedLoop = true;
                    break;
                }
            }
            if (!foundSerializedLoop) {
                helper.fail("Closed path loop did not persist its ordered repeated endpoint");
                return;
            }
            helper.succeed();
        });
    }
}
