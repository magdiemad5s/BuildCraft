/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import buildcraft.core.BCCoreItems;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime coverage for the shared BuildCraft tile removal lifecycle. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class RemovalLifecycleGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final int TEST_FLUID_AMOUNT = 500;

    private RemovalLifecycleGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void tankContentsAndBlockItemDropExactlyOnceAcrossRemovalPaths(GameTestHelper helper) {
        BlockPos survivalPos = new BlockPos(1, 1, 1);
        BlockPos creativePos = new BlockPos(4, 1, 1);
        BlockPos explosionPos = new BlockPos(1, 1, 4);
        BlockPos replacementPos = new BlockPos(4, 1, 4);

        helper.setBlock(survivalPos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(creativePos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(explosionPos, BCFactoryBlocks.TANK_BLOCK.get());
        helper.setBlock(replacementPos, BCFactoryBlocks.TANK_BLOCK.get());

        helper.runAfterDelay(1, () -> {
            fillTank(helper, survivalPos);
            fillTank(helper, creativePos);
            fillTank(helper, explosionPos);
            fillTank(helper, replacementPos);

            destroyAsPlayer(helper, survivalPos, false);
            destroyAsPlayer(helper, creativePos, true);
            explode(helper, explosionPos);
            helper.getLevel().setBlockAndUpdate(
                helper.absolutePos(replacementPos), Blocks.STONE.defaultBlockState()
            );
        });

        helper.runAfterDelay(5, () -> {
            assertDrops(helper, survivalPos, "survival", 1, 1);
            assertDrops(helper, creativePos, "creative", 0, 1);
            assertDrops(helper, explosionPos, "explosion", 1, 1);
            assertDrops(helper, replacementPos, "programmatic replacement", 0, 1);

            if (!helper.getBlockState(survivalPos).isAir()
                || !helper.getBlockState(creativePos).isAir()
                || !helper.getBlockState(explosionPos).isAir()) {
                helper.fail("A player/explosion removal path left its tank block in the test world");
                return;
            }
            if (!helper.getBlockState(replacementPos).is(Blocks.STONE)) {
                helper.fail("Programmatic replacement did not leave the replacement block in place");
                return;
            }
            helper.succeed();
        });
    }

    private static void fillTank(GameTestHelper helper, BlockPos relativePos) {
        BlockEntity blockEntity = helper.getBlockEntity(relativePos);
        if (!(blockEntity instanceof TileTank tank)) {
            helper.fail("Tank block entity was not created at " + relativePos);
            return;
        }
        int accepted = tank.getCapability(ForgeCapabilities.FLUID_HANDLER)
            .map(handler -> handler.fill(
                new FluidStack(Fluids.WATER, TEST_FLUID_AMOUNT), FluidAction.EXECUTE
            ))
            .orElse(0);
        if (accepted != TEST_FLUID_AMOUNT) {
            helper.fail("Tank at " + relativePos + " accepted " + accepted + " mB instead of " + TEST_FLUID_AMOUNT);
        }
    }

    private static void destroyAsPlayer(GameTestHelper helper, BlockPos relativePos, boolean creative) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolutePos);
        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = creative;

        boolean removed = state.getBlock().onDestroyedByPlayer(
            state, helper.getLevel(), absolutePos, player, !creative, state.getFluidState()
        );
        if (!removed) {
            helper.fail((creative ? "Creative" : "Survival") + " player failed to remove the tank");
            return;
        }
        if (!creative) {
            state.getBlock().destroy(helper.getLevel(), absolutePos, state);
            state.getBlock().playerDestroy(
                helper.getLevel(), player, absolutePos, state, blockEntity, new ItemStack(Items.DIAMOND_PICKAXE)
            );
        }
    }

    private static void explode(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        Explosion explosion = new Explosion(
            helper.getLevel(), null,
            absolutePos.getX() + 0.5D, absolutePos.getY() + 0.5D, absolutePos.getZ() + 0.5D,
            1.0F, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY
        );
        explosion.getToBlow().add(absolutePos);
        explosion.finalizeExplosion(false);
    }

    private static void assertDrops(
        GameTestHelper helper, BlockPos relativePos, String path, int expectedBlocks, int expectedContents
    ) {
        int blocks = countItemEntities(helper, relativePos, BCFactoryBlocks.TANK_BLOCK.get().asItem());
        int contents = countItemEntities(helper, relativePos, BCCoreItems.FRAGILE_FLUID_SHARD.get());
        if (blocks != expectedBlocks || contents != expectedContents) {
            helper.fail(
                path + " removal dropped " + blocks + " tank item(s) and " + contents
                    + " fluid shard(s); expected " + expectedBlocks + " and " + expectedContents
            );
        }
    }

    private static int countItemEntities(GameTestHelper helper, BlockPos relativeCenter, Item item) {
        AABB area = new AABB(helper.absolutePos(relativeCenter)).inflate(1.1D);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
            .filter(entity -> entity.isAlive() && entity.getItem().is(item))
            .mapToInt(entity -> entity.getItem().getCount())
            .sum();
    }
}
