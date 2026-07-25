/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.client.render;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.energy.tile.TileDynamoMJ;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Block-entity renderer for the original directional piston model.
 *
 * <p>The legacy expression model cannot be consumed by the vanilla 1.20.1 model loader, so this
 * renderer reproduces its four cuboids, UV regions, output-facing rotation, and piston travel.</p>
 */
public class RenderDynamoMJ implements BlockEntityRenderer<TileDynamoMJ> {
    private static final ResourceLocation BACK =
        new ResourceLocation("buildcraftenergy", "blocks/mj_dynamo/back");
    private static final ResourceLocation FRONT =
        new ResourceLocation("buildcraftenergy", "blocks/mj_dynamo/front");
    private static final ResourceLocation SIDE =
        new ResourceLocation("buildcraftenergy", "blocks/mj_dynamo/side");
    private static final ResourceLocation CHAMBER =
        new ResourceLocation("buildcraftlib", "blocks/engine/chamber_base");

    public RenderDynamoMJ(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        TileDynamoMJ dynamo,
        float partialTicks,
        PoseStack pose,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        TextureAtlasSprite back = sprite(BACK);
        TextureAtlasSprite front = sprite(FRONT);
        TextureAtlasSprite side = sprite(SIDE);
        TextureAtlasSprite chamber = sprite(CHAMBER);
        TextureAtlasSprite trunk = sprite(trunkTexture(dynamo.getPowerStage()));

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        rotateFromUp(pose, dynamo.getCurrentDirection());

        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        // Fixed 16x4x16 base.
        cube(
            consumer, matrix, normal, packedLight, packedOverlay,
            -0.5F, -0.5F, -0.5F, 0.5F, -0.25F, 0.5F,
            back, 0, 0, 16, 16,
            side, 0, 0, 16, 4
        );

        // Fixed 8x12x8 stage-coloured trunk.
        cube(
            consumer, matrix, normal, packedLight, packedOverlay,
            -0.25F, -0.25F, -0.25F, 0.25F, 0.5F, 0.25F,
            trunk, 0, 0, 8, 8,
            trunk, 8, 0, 16, 12
        );

        float progress = dynamo.getProgressClient(partialTicks);
        float progressSize = progress > 0.5F
            ? (1.0F - progress) * 15.99F
            : progress * 15.99F;
        float offset = progressSize / 16.0F;

        // Moving 12x4x12 head.
        cube(
            consumer, matrix, normal, packedLight, packedOverlay,
            -0.375F, -0.25F + offset, -0.375F,
            0.375F, offset, 0.375F,
            front, 0, 0, 12, 12,
            front, 0, 12, 12, 16
        );

        // Expanding 10xN x10 chamber between the base and moving head.
        if (progressSize > 0.001F) {
            fourSides(
                consumer, matrix, normal, packedLight, packedOverlay,
                -0.3125F, -0.25F, -0.3125F,
                0.3125F, -0.25F + offset, 0.3125F,
                chamber, 3, 0, 13, progressSize
            );
        }

        pose.popPose();
    }

    private static ResourceLocation trunkTexture(EnumPowerStage stage) {
        String name = switch (stage) {
            case GREEN -> "trunk_green";
            case YELLOW -> "trunk_yellow";
            case RED -> "trunk_red";
            case OVERHEAT -> "trunk_overheat";
            default -> "trunk_blue";
        };
        return new ResourceLocation("buildcraftlib", "blocks/engine/" + name);
    }

    private static TextureAtlasSprite sprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }

    private static void rotateFromUp(PoseStack pose, net.minecraft.core.Direction direction) {
        switch (direction) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180));
            case EAST -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.mulPose(Axis.ZN.rotationDegrees(90));
            }
            case NORTH -> pose.mulPose(Axis.XN.rotationDegrees(90));
            case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case UP -> {
            }
        }
    }

    private static void cube(
        VertexConsumer consumer,
        Matrix4f matrix,
        Matrix3f normal,
        int light,
        int overlay,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        TextureAtlasSprite cap,
        float capU1,
        float capV1,
        float capU2,
        float capV2,
        TextureAtlasSprite side,
        float sideU1,
        float sideV1,
        float sideU2,
        float sideV2
    ) {
        // Down.
        quad(
            consumer, matrix, normal, light, overlay, cap, capU1, capV1, capU2, capV2, 0, -1, 0,
            x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2
        );
        // Up.
        quad(
            consumer, matrix, normal, light, overlay, cap, capU1, capV1, capU2, capV2, 0, 1, 0,
            x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1
        );
        fourSides(
            consumer, matrix, normal, light, overlay,
            x1, y1, z1, x2, y2, z2,
            side, sideU1, sideV1, sideU2, sideV2
        );
    }

    private static void fourSides(
        VertexConsumer consumer,
        Matrix4f matrix,
        Matrix3f normal,
        int light,
        int overlay,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        TextureAtlasSprite sprite,
        float u1,
        float v1,
        float u2,
        float v2
    ) {
        // North.
        quad(
            consumer, matrix, normal, light, overlay, sprite, u1, v1, u2, v2, 0, 0, -1,
            x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1
        );
        // South.
        quad(
            consumer, matrix, normal, light, overlay, sprite, u1, v1, u2, v2, 0, 0, 1,
            x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2
        );
        // West.
        quad(
            consumer, matrix, normal, light, overlay, sprite, u1, v1, u2, v2, -1, 0, 0,
            x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1
        );
        // East.
        quad(
            consumer, matrix, normal, light, overlay, sprite, u1, v1, u2, v2, 1, 0, 0,
            x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2
        );
    }

    private static void quad(
        VertexConsumer consumer,
        Matrix4f matrix,
        Matrix3f normal,
        int light,
        int overlay,
        TextureAtlasSprite sprite,
        float u1,
        float v1,
        float u2,
        float v2,
        float nx,
        float ny,
        float nz,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4
    ) {
        float minU = sprite.getU(u1);
        float maxU = sprite.getU(u2);
        float minV = sprite.getV(v1);
        float maxV = sprite.getV(v2);
        vertex(consumer, matrix, normal, light, overlay, x1, y1, z1, minU, maxV, nx, ny, nz);
        vertex(consumer, matrix, normal, light, overlay, x2, y2, z2, minU, minV, nx, ny, nz);
        vertex(consumer, matrix, normal, light, overlay, x3, y3, z3, maxU, minV, nx, ny, nz);
        vertex(consumer, matrix, normal, light, overlay, x4, y4, z4, maxU, maxV, nx, ny, nz);
    }

    private static void vertex(
        VertexConsumer consumer,
        Matrix4f matrix,
        Matrix3f normal,
        int light,
        int overlay,
        float x,
        float y,
        float z,
        float u,
        float v,
        float nx,
        float ny,
        float nz
    ) {
        consumer.vertex(matrix, x, y, z)
            .color(1.0F, 1.0F, 1.0F, 1.0F)
            .uv(u, v)
            .overlayCoords(overlay)
            .uv2(light)
            .normal(normal, nx, ny, nz)
            .endVertex();
    }
}
