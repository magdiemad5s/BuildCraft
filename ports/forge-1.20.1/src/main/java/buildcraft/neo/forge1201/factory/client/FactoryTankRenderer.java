package buildcraft.neo.forge1201.factory.client;

import buildcraft.neo.forge1201.factory.FactoryTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Renders the locally stored fluid inside the Factory Tank's glass frame. */
public final class FactoryTankRenderer implements BlockEntityRenderer<FactoryTankBlockEntity> {
    private static final float MIN_XZ = 0.13F;
    private static final float MAX_XZ = 0.86F;
    private static final float MIN_Y = 0.01F;
    private static final float MAX_Y = 0.99F;
    private static final float MIN_VISIBLE_HEIGHT = 0.01F;

    public FactoryTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        FactoryTankBlockEntity tank,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        FluidStack fluid = tank.getLocalFluid();
        int capacity = tank.getLocalCapacity();
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }

        float fillFraction = Math.min(1.0F, (float) fluid.getAmount() / capacity);
        float maxY = Math.min(MAX_Y, MIN_Y + Math.max(MIN_VISIBLE_HEIGHT, (MAX_Y - MIN_Y) * fillFraction));
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extension.getStillTexture(fluid));
        int color = extension.getTintColor(fluid) | 0xFF000000;

        addCuboid(
            buffers.getBuffer(RenderType.translucent()),
            poseStack.last(),
            MIN_XZ,
            MIN_Y,
            MIN_XZ,
            MAX_XZ,
            maxY,
            MAX_XZ,
            sprite,
            color,
            packedLight,
            packedOverlay
        );
    }

    private static void addCuboid(
        VertexConsumer vertices,
        PoseStack.Pose pose,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        TextureAtlasSprite sprite,
        int color,
        int packedLight,
        int packedOverlay
    ) {
        quad(vertices, pose, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0, 0, -1, sprite, color, packedLight, packedOverlay);
        quad(vertices, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1, sprite, color, packedLight, packedOverlay);
        quad(vertices, pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0, sprite, color, packedLight, packedOverlay);
        quad(vertices, pose, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1, 0, 0, sprite, color, packedLight, packedOverlay);
        quad(vertices, pose, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0, 1, 0, sprite, color, packedLight, packedOverlay);
        quad(vertices, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0, sprite, color, packedLight, packedOverlay);
    }

    private static void quad(
        VertexConsumer vertices,
        PoseStack.Pose pose,
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
        float z4,
        float normalX,
        float normalY,
        float normalZ,
        TextureAtlasSprite sprite,
        int color,
        int packedLight,
        int packedOverlay
    ) {
        vertex(vertices, pose, x1, y1, z1, sprite.getU0(), sprite.getV1(), normalX, normalY, normalZ, color, packedLight, packedOverlay);
        vertex(vertices, pose, x2, y2, z2, sprite.getU1(), sprite.getV1(), normalX, normalY, normalZ, color, packedLight, packedOverlay);
        vertex(vertices, pose, x3, y3, z3, sprite.getU1(), sprite.getV0(), normalX, normalY, normalZ, color, packedLight, packedOverlay);
        vertex(vertices, pose, x4, y4, z4, sprite.getU0(), sprite.getV0(), normalX, normalY, normalZ, color, packedLight, packedOverlay);
    }

    private static void vertex(
        VertexConsumer vertices,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        float normalX,
        float normalY,
        float normalZ,
        int color,
        int packedLight,
        int packedOverlay
    ) {
        vertices.vertex(pose.pose(), x, y, z)
            .color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(pose.normal(), normalX, normalY, normalZ)
            .endVertex();
    }
}
