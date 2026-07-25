/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.builders.snapshot;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Sparse composite-blueprint cell that deliberately leaves the target world unchanged.
 *
 * <p>Using ordinary air for the gaps between linked blueprints would make an excavating Builder destroy every block
 * in those gaps. This explicit no-op entry preserves the released nested-blueprint behavior after composition is
 * flattened into the modern snapshot format.</p>
 */
public final class SchematicBlockIgnore implements ISchematicBlock {
    public static boolean predicate(SchematicBlockContext context) {
        return false;
    }

    @Override
    public void init(SchematicBlockContext context) {
    }

    @Override
    public SchematicBlockIgnore getRotated(Rotation rotation) {
        return new SchematicBlockIgnore();
    }

    @Override
    public boolean canBuild(Level level, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean build(Level level, BlockPos blockPos) {
        return true;
    }

    @Override
    public boolean buildWithoutChecks(Level level, BlockPos blockPos) {
        return true;
    }

    @Override
    public boolean isBuilt(Level level, BlockPos blockPos) {
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) throws InvalidInputDataException {
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SchematicBlockIgnore;
    }

    @Override
    public int hashCode() {
        return SchematicBlockIgnore.class.hashCode();
    }
}
