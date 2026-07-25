/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.UUID;

import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.builders.snapshot.SchematicBlockDefault;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime regression coverage for claim/protection cancellation of Builder placements. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class BuilderPlacementGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private BuilderPlacementGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void cancelledPlacementRestoresBlockStateAndBlockEntityNbt(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        BlockPos actorPos = new BlockPos(3, 1, 1);
        BlockState sourceState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        BlockState targetState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        helper.setBlock(sourcePos, sourceState);
        helper.setBlock(targetPos, targetState);

        helper.runAfterDelay(2, () -> {
            ChestBlockEntity sourceChest = chest(helper, sourcePos, "source");
            ChestBlockEntity targetChest = chest(helper, targetPos, "target");
            if (sourceChest == null || targetChest == null) {
                return;
            }
            sourceChest.setItem(0, new ItemStack(Items.DIAMOND, 3));
            sourceChest.setChanged();
            targetChest.setItem(0, new ItemStack(Items.GOLD_INGOT, 5));
            targetChest.setChanged();

            BlockPos absoluteSource = helper.absolutePos(sourcePos);
            BlockPos absoluteTarget = helper.absolutePos(targetPos);
            BlockPos absoluteActor = helper.absolutePos(actorPos);
            CompoundTag originalTargetNbt = targetChest.serializeNBT().copy();

            SchematicBlockDefault schematic = new SchematicBlockDefault();
            schematic.init(new SchematicBlockContext(
                helper.getLevel(),
                absoluteSource,
                absoluteSource,
                sourceState,
                sourceState.getBlock()
            ));

            GameProfile owner = new GameProfile(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "BuildCraftTest"
            );
            CancellingPlacementListener listener = new CancellingPlacementListener(absoluteTarget);
            MinecraftForge.EVENT_BUS.register(listener);
            boolean built;
            try {
                built = schematic.build(helper.getLevel(), absoluteTarget, owner, absoluteActor);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }

            if (built) {
                helper.fail("A cancelled Builder placement reported success");
                return;
            }
            if (!listener.sawPlacement) {
                helper.fail("Builder did not emit Forge EntityPlaceEvent");
                return;
            }
            if (!owner.getId().equals(listener.actorOwnerId)) {
                helper.fail("Placement event did not use the Builder owner's fake-player profile");
                return;
            }
            if (!absoluteActor.equals(listener.actorPos)) {
                helper.fail("Placement fake player was not positioned at the Builder");
                return;
            }
            if (!sourceState.equals(listener.proposedState) || !targetState.equals(listener.replacedState)) {
                helper.fail("Placement event exposed the wrong proposed or replaced block state");
                return;
            }
            if (!targetState.equals(helper.getLevel().getBlockState(absoluteTarget))) {
                helper.fail("Cancelled placement did not restore the original block state");
                return;
            }

            BlockEntity restored = helper.getLevel().getBlockEntity(absoluteTarget);
            if (!(restored instanceof ChestBlockEntity restoredChest)) {
                helper.fail("Cancelled placement did not restore the original chest block entity");
                return;
            }
            if (!originalTargetNbt.equals(restoredChest.serializeNBT())
                || !restoredChest.getItem(0).is(Items.GOLD_INGOT)
                || restoredChest.getItem(0).getCount() != 5) {
                helper.fail("Cancelled placement changed the original block-entity NBT or inventory");
                return;
            }
            helper.succeed();
        });
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos, String label) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            return chest;
        }
        helper.fail("Expected " + label + " chest at " + pos + " but found " + blockEntity);
        return null;
    }

    private static final class CancellingPlacementListener {
        private final BlockPos target;
        private boolean sawPlacement;
        private UUID actorOwnerId;
        private BlockPos actorPos;
        private BlockState proposedState;
        private BlockState replacedState;

        private CancellingPlacementListener(BlockPos target) {
            this.target = target;
        }

        @SubscribeEvent
        public void onPlace(BlockEvent.EntityPlaceEvent event) {
            if (!target.equals(event.getPos())) {
                return;
            }
            sawPlacement = true;
            proposedState = event.getPlacedBlock();
            replacedState = event.getBlockSnapshot().getReplacedBlock();
            if (event.getEntity() instanceof FakePlayer fakePlayer) {
                actorOwnerId = fakePlayer.getGameProfile().getId();
                actorPos = fakePlayer.blockPosition();
            }
            event.setCanceled(true);
        }
    }
}
