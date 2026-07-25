/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import buildcraft.factory.tile.TileTank;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.FluidSmoother.FluidStackInterp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/** Renders a tank's local fluid, hiding only faces that physically touch a compatible vertical neighbour. */
public final class RenderTank implements BlockEntityRenderer<TileTank> {
    private static final Vec3 MIN = new Vec3(0.13, 0.01, 0.13);
    private static final Vec3 MAX = new Vec3(0.86, 0.99, 0.86);
    private static final Vec3 MIN_CONNECTED = new Vec3(0.13, 0, 0.13);
    private static final Vec3 MAX_CONNECTED = new Vec3(0.86, 1 - 1e-5, 0.86);

    public RenderTank(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        TileTank tile,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        FluidStackInterp renderState = tile.getFluidForRender(partialTicks);
        int capacity = tile.tank.getCapacity();
        if (!isRenderable(renderState, capacity)) {
            return;
        }

        FluidStack fluid = renderState.fluid;
        double amount = Math.min(capacity, renderState.amount);
        boolean connectedUp = isFullyConnected(tile, renderState, Direction.UP, partialTicks);
        boolean connectedDown = isFullyConnected(tile, renderState, Direction.DOWN, partialTicks);
        boolean[] renderedFaces = {true, true, true, true, true, true};
        renderedFaces[Direction.DOWN.ordinal()] = !connectedDown;
        renderedFaces[Direction.UP.ordinal()] = !connectedUp;

        Vec3 min = connectedDown ? MIN_CONNECTED : MIN;
        Vec3 max = connectedUp ? MAX_CONNECTED : MAX;
        int existingBlockLight = packedLight & 0x0000F0;
        int skyLight = packedLight & 0xF00000;
        int fluidBlockLight = Math.min(15, Math.max(0, fluid.getFluid().getFluidType().getLightLevel(fluid))) << 4;
        int combinedLight = skyLight | Math.max(existingBlockLight, fluidBlockLight);

        poseStack.pushPose();
        try {
            VertexConsumer vertices = buffers.getBuffer(RenderType.translucent());
            FluidRenderer.vertex.lighti(combinedLight);
            FluidRenderer.vertex.overlay(packedOverlay);
            FluidRenderer.renderFluid(
                FluidSpriteType.STILL,
                fluid,
                amount,
                capacity,
                min,
                max,
                vertices,
                poseStack.last(),
                renderedFaces
            );
        } finally {
            poseStack.popPose();
        }
    }

    private static boolean isRenderable(FluidStackInterp state, int capacity) {
        return state != null
            && state.fluid != null
            && !state.fluid.isEmpty()
            && capacity > 0
            && Double.isFinite(state.amount)
            && state.amount > 0;
    }

    private static boolean isFullyConnected(
        TileTank localTank,
        FluidStackInterp localState,
        Direction face,
        float partialTicks
    ) {
        if (!localTank.hasLevel()) {
            return false;
        }
        BlockPos neighbourPos = localTank.getBlockPos().offset(face.getNormal());
        BlockEntity blockEntity = localTank.getLevel().getBlockEntity(neighbourPos);
        if (!(blockEntity instanceof TileTank neighbour)
            || !TileTank.canTanksConnect(localTank, neighbour, face)) {
            return false;
        }

        FluidStackInterp neighbourState = neighbour.getFluidForRender(partialTicks);
        int localCapacity = localTank.tank.getCapacity();
        int neighbourCapacity = neighbour.tank.getCapacity();
        if (!isRenderable(localState, localCapacity)
            || !isRenderable(neighbourState, neighbourCapacity)
            || !FluidCompatRegistry.areEquivalent(localState.fluid, neighbourState.fluid)) {
            return false;
        }

        boolean gaseous = localState.fluid.getFluid().getFluidType().isLighterThanAir();
        boolean localFull = localState.amount >= localCapacity;
        boolean neighbourFull = neighbourState.amount >= neighbourCapacity;
        if (gaseous) {
            return face == Direction.UP ? neighbourFull : localFull;
        }
        return face == Direction.UP ? localFull : neighbourFull;
    }
}