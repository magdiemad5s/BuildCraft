/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.core.block;

import buildcraft.core.blockEntity.TilePowerConsumerTester;
import buildcraft.lib.block.BlockBCTile_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Developer diagnostics block retained for exact legacy registry and world
 * compatibility. It accepts MJ on every face and records transfer totals.
 */
public class BlockPowerConsumerTester extends BlockBCTile_Neptune {
    public BlockPowerConsumerTester() {
        super();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePowerConsumerTester(pos, state);
    }
}