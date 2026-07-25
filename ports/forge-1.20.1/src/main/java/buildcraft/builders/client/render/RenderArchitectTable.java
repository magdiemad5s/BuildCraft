/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class RenderArchitectTable implements BlockEntityRenderer<TileArchitectTable> {
   
	public RenderArchitectTable(BlockEntityRendererProvider.Context bpc) {
	}
	
	@Override
    public void render(TileArchitectTable tile, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        if (!tile.markerBox && tile.subBlueprints.isEmpty()) {
            return;
        }
		
//        Minecraft.getInstance().getProfiler().push("bc");
//        Minecraft.getInstance().getProfiler().push("architect_table");

        matrix.pushPose();
		VertexConsumer bb = buffer.getBuffer(RenderType.cutout());
		Matrix4f pose = matrix.last().pose();
		Matrix3f normal = matrix.last().normal();
		BlockPos pos = tile.getBlockPos();
		Vec3 renderOrigin = Vec3.atLowerCornerOf(pos);
//        Minecraft.getInstance().getProfiler().push("box");
        if (tile.markerBox) {
            LaserBoxRenderer.renderLaserBoxDynamicRelative(
                tile.box, BuildCraftLaserManager.STRIPES_READ, renderOrigin, pose, normal, bb, true
            );
        }
        Vec3 start = Vec3.atCenterOf(pos);
        for (BlockPos subBlueprint : tile.subBlueprints) {
            if (!subBlueprint.equals(pos)) {
                LaserData_BC8 data = new LaserData_BC8(
                    BuildCraftLaserManager.STRIPES_READ, start, Vec3.atCenterOf(subBlueprint), 1 / 32.0, true
                );
                LaserRenderer_BC8.renderLaserDynamicRelative(pose, normal, data, renderOrigin, bb);
            }
        }
//        Minecraft.getInstance().getProfiler().pop();

        matrix.popPose();

//        Minecraft.getInstance().getProfiler().pop();
//        Minecraft.getInstance().getProfiler().pop();
    }

    
    @Override
    public boolean shouldRenderOffScreen(TileArchitectTable te) {
        return true;
    }
}
