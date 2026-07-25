/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */

package buildcraft.transport.client.render;

import java.util.function.Function;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IPipeFlowRenderer;
import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.model.ModelUtil.UvFaceData;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.misc.MathUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.transport.pipe.flow.PipeFlowRedstoneFlux;
import buildcraft.transport.pipe.flow.PipeFlowRedstoneFlux.Section;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Renders the red Forge Energy stream used by the legacy RF pipes. */
@OnlyIn(Dist.CLIENT)
public enum PipeFlowRendererRf implements IPipeFlowRenderer<PipeFlowRedstoneFlux> {
    INSTANCE;

    private static TextureAtlasSprite energySprite;

    @Override
    public void render(
        PipeFlowRedstoneFlux flow,
        float partialTicks,
        PoseStack matrix,
        MultiBufferSource buffer,
        int combinedLight,
        int combinedOverlay
    ) {
        if (energySprite == null) {
            Function<ResourceLocation, TextureAtlasSprite> atlas =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            energySprite = atlas.apply(
                new ResourceLocation("buildcrafttransport:pipes/power_flow_overload")
            );
        }

        Matrix4f pose = matrix.last().pose();
        Matrix3f normal = matrix.last().normal();
        VertexConsumer vertices = buffer.getBuffer(RenderType.cutout());

        double centrePower = 0;
        double[] power = new double[Direction.values().length];
        for (Direction side : Direction.values()) {
            Section section = flow.getSection(side);
            power[side.ordinal()] = section.displayPower / (double) MjAPI.MJ;
            centrePower = Math.max(centrePower, power[side.ordinal()]);
        }
        if (centrePower <= 0) {
            return;
        }

        for (Direction side : Direction.values()) {
            if (!flow.pipe.isConnected(side)) {
                continue;
            }
            Section section = flow.getSection(side);
            double offset = computeOffset(
                section.clientDisplayFlowLast, section.clientDisplayFlow, partialTicks
            );
            renderSide(
                side, power[side.ordinal()], centrePower, offset, pose, normal, vertices
            );
        }

        Vec3 last = flow.clientDisplayFlowCentreLast;
        Vec3 current = flow.clientDisplayFlowCentre;
        Vec3 offset = new Vec3(
            computeOffset(last.x, current.x, partialTicks),
            computeOffset(last.y, current.y, partialTicks),
            computeOffset(last.z, current.z, partialTicks)
        );
        renderCentre(centrePower, offset, pose, normal, vertices);
    }

    private static double computeOffset(double tick0, double tick1, float partialTicks) {
        if (tick0 + 8 < tick1) {
            tick0 += 16;
        } else if (tick1 + 8 < tick0) {
            tick1 += 16;
        }
        double offset = MathUtil.interp(partialTicks, tick0, tick1);
        return offset >= 16 ? offset - 16 : offset;
    }

    private static void renderSide(
        Direction side,
        double power,
        double centrePower,
        double offset,
        Matrix4f pose,
        Matrix3f normal,
        VertexConsumer vertices
    ) {
        if (power <= 0) {
            return;
        }
        double radius = Math.min(0.248, 0.248 * power);
        double centreRadius = 0.252 - 0.248 * centrePower;

        Vec3 centre = VecUtil.offset(
            VecUtil.VEC_HALF, side, 0.25 + 0.125 - centreRadius / 2
        );
        Vec3 radiusVector = VecUtil.replaceValue(
            new Vec3(radius, radius, radius),
            side.getAxis(),
            0.125 + centreRadius / 2
        );

        Vector3f centreF = new Vector3f(
            (float) centre.x, (float) centre.y, (float) centre.z
        );
        Vector3f radiusF = new Vector3f(
            (float) radiusVector.x, (float) radiusVector.y, (float) radiusVector.z
        );
        UvFaceData uvs = new UvFaceData();
        for (Direction face : Direction.values()) {
            if (face == side.getOpposite()) {
                continue;
            }
            AABB box = new AABB(
                centre.subtract(radiusVector).scale(0.5),
                centre.add(radiusVector).scale(0.5)
            ).move(
                VecUtil.offset(
                    Vec3.ZERO,
                    side,
                    offset * side.getAxisDirection().getStep() / 32
                )
            );
            ModelUtil.mapBoxToUvs(box, face, uvs);

            MutableQuad quad = ModelUtil.createFace(face, centreF, radiusF, uvs);
            quad.texFromSprite(energySprite);
            quad.lighti(15, 15);
            quad.render(pose, normal, vertices);
        }
    }

    private static void renderCentre(
        double power,
        Vec3 offset,
        Matrix4f pose,
        Matrix3f normal,
        VertexConsumer vertices
    ) {
        float radius = (float) Math.min(0.248, 0.248 * power);
        Vector3f centre = new Vector3f(0.5f, 0.5f, 0.5f);
        Vector3f radiusVector = new Vector3f(radius, radius, radius);
        UvFaceData uvs = new UvFaceData();

        for (Direction face : Direction.values()) {
            AABB box = new AABB(
                new Vec3(0.5 - radius, 0.5 - radius, 0.5 - radius).scale(0.5),
                new Vec3(0.5 + radius, 0.5 + radius, 0.5 + radius).scale(0.5)
            ).move(offset.scale(1 / 32.0));
            ModelUtil.mapBoxToUvs(box, face, uvs);

            MutableQuad quad = ModelUtil.createFace(face, centre, radiusVector, uvs);
            quad.texFromSprite(energySprite);
            quad.lighti(15, 15);
            quad.render(pose, normal, vertices);
        }
    }
}
