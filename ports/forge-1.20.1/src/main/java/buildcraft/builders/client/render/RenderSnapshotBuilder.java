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
import org.joml.Vector3f;

import buildcraft.builders.BCBuildersSprites;
import buildcraft.builders.snapshot.ITileForSnapshotBuilder;
import buildcraft.builders.snapshot.SnapshotBuilder;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.render.WorldRenderMath;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.MathUtil;
import buildcraft.lib.misc.VecUtil;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderSnapshotBuilder {
    public static <T extends ITileForSnapshotBuilder> void render(
            SnapshotBuilder<T> snapshotBuilder,
            Level world,
            BlockPos tilePos,
            float partialTicks,
            PoseStack matrix,
            MultiBufferSource buffer,
            ItemRenderer itemRenderer
    ) {
        Vec3 renderOrigin = Vec3.atLowerCornerOf(tilePos);
        Vec3 builderStart = renderOrigin.add(0.5, 1, 0.5);
        for (SnapshotBuilder<T>.PlaceTask placeTask : snapshotBuilder.clientPlaceTasks) {
            Vec3 prevPos = snapshotBuilder.prevClientPlaceTasks.stream()
                .filter(renderTaskLocal -> renderTaskLocal.pos.equals(placeTask.pos))
                .map(snapshotBuilder::getPlaceTaskItemPos)
                .findFirst()
                .orElse(builderStart);
            Vec3 worldPos = prevPos.add(snapshotBuilder.getPlaceTaskItemPos(placeTask).subtract(prevPos).scale(partialTicks));
            Vec3 localPos = WorldRenderMath.relative(worldPos, renderOrigin);
            int itemLight = LevelRenderer.getLightColor(world, BlockPos.containing(worldPos));

            matrix.pushPose();
            matrix.translate(localPos.x, localPos.y, localPos.z);
            int i = 0;
            for (ItemStack item : placeTask.items) {
                itemRenderer.renderStatic(
                    item,
                    ItemDisplayContext.GROUND,
                    itemLight,
                    OverlayTexture.NO_OVERLAY,
                    matrix,
                    buffer,
                    world,
                    i++
                );
            }
            matrix.popPose();
        }

        Vec3 robotPos = snapshotBuilder.robotPos;
        if (robotPos == null) {
            return;
        }
        if (snapshotBuilder.prevRobotPos != null) {
            robotPos = snapshotBuilder.prevRobotPos.add(
                robotPos.subtract(snapshotBuilder.prevRobotPos).scale(partialTicks)
            );
        }

        Vec3 localRobotPos = WorldRenderMath.relative(robotPos, renderOrigin);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        Matrix4f pose = matrix.last().pose();
        Matrix3f normal = matrix.last().normal();
        Vector3f center = new Vector3f((float) localRobotPos.x, (float) localRobotPos.y, (float) localRobotPos.z);
        Vector3f radius = new Vector3f(4 / 16F, 4 / 16F, 4 / 16F);
        int robotLight = LevelRenderer.getLightColor(world, BlockPos.containing(robotPos));
        int faceIndex = 0;
        for (Direction face : Direction.values()) {
            MutableQuad quad = ModelUtil.createFace(
                face,
                center,
                radius,
                new ModelUtil.UvFaceData(
                    BCBuildersSprites.ROBOT.getInterpU((faceIndex * 8) / 64D),
                    BCBuildersSprites.ROBOT.getInterpV(0),
                    BCBuildersSprites.ROBOT.getInterpU(((faceIndex + 1) * 8) / 64D),
                    BCBuildersSprites.ROBOT.getInterpV(8 / 64D)
                )
            );
            quad.lighti(robotLight).render(pose, normal, vertexConsumer);
            faceIndex++;
        }

        for (SnapshotBuilder<T>.BreakTask breakTask : snapshotBuilder.clientBreakTasks) {
            long target = breakTask.getTarget();
            double progress = target <= 0 ? 1 : MathUtil.clamp(breakTask.power * 1D / target, 0D, 1D);
            int powerIndex = (int) Math.round(progress * (BuildCraftLaserManager.POWERS.length - 1));
            LaserData_BC8 data = new LaserData_BC8(
                BuildCraftLaserManager.POWERS[powerIndex],
                robotPos.subtract(0, 0.27, 0),
                Vec3.atLowerCornerOf(breakTask.pos).add(VecUtil.VEC_HALF),
                1 / 16D
            );
            LaserRenderer_BC8.renderLaserDynamicRelative(pose, normal, data, renderOrigin, vertexConsumer);
        }
    }
}
