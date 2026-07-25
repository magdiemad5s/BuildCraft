/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import buildcraft.builders.tile.TileBuilder;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class RenderBuilder implements BlockEntityRenderer<TileBuilder> {
    private static final double OFFSET = 0.1;

	protected final ItemRenderer itemRenderer; 
	
	public RenderBuilder(BlockEntityRendererProvider.Context bpc) {
		itemRenderer = bpc.getItemRenderer();
	}
    
    @Override
    public void render(@Nonnull TileBuilder tile, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        Minecraft.getInstance().getProfiler().push("bc");
        Minecraft.getInstance().getProfiler().push("builder");
        matrix.pushPose();
		VertexConsumer bb = buffer.getBuffer(RenderType.cutout());
		BlockPos pos = tile.getBlockPos();
		Vec3 renderOrigin = Vec3.atLowerCornerOf(pos);
		
		Matrix4f pose = matrix.last().pose();
		Matrix3f normal = matrix.last().normal();
        Minecraft.getInstance().getProfiler().push("box");
        Box box = tile.getBox();
        LaserBoxRenderer.renderLaserBoxDynamicRelative(
            box, BuildCraftLaserManager.STRIPES_WRITE, renderOrigin, pose, normal, bb, true
        );
        Minecraft.getInstance().getProfiler().popPush("path");
        
        List<BlockPos> path = tile.path;
        if (path != null) {
            BlockPos last = null;
            for (BlockPos p : path) {
                if (last != null) {
                    Vec3 from = Vec3.atCenterOf(last);
                    Vec3 to = Vec3.atCenterOf(p);
                    Vec3 one = offset(from, to);
                    Vec3 two = offset(to, from);
                    LaserData_BC8 data = new LaserData_BC8(BuildCraftLaserManager.STRIPES_WRITE_DIRECTION, one, two, 1 / 16.1, true);
                    LaserRenderer_BC8.renderLaserDynamicRelative(pose, normal, data, renderOrigin, bb);
                }
                last = p;
            }
        }

        Minecraft.getInstance().getProfiler().pop();

        if (tile.getBuilder() != null) {
            RenderSnapshotBuilder.render(tile.getBuilder(), tile.getLevel(), tile.getBlockPos(), partialTicks, matrix, buffer, itemRenderer);
        }
        matrix.popPose();
        Minecraft.getInstance().getProfiler().pop();
        Minecraft.getInstance().getProfiler().pop();
    }

    private static Vec3 offset(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        return from.add(VecUtil.scale(dir, OFFSET));
    }

    @Override
    public boolean shouldRenderOffScreen(TileBuilder te) {
        return true;
    }

	@Override
	public int getViewDistance() {
		return 256;
	}
    
    
}
