/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0
 */
package buildcraft.builders.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BuilderFarCoordinateRenderContractTest {
    @Test
    void builderAndArchitectLasersUseTheirTileAsTheStableRenderOrigin() throws IOException {
        String builder = read("src/main/java/buildcraft/builders/client/render/RenderBuilder.java");
        assertTrue(builder.contains("Vec3 renderOrigin = Vec3.atLowerCornerOf(pos)"));
        assertTrue(builder.contains("renderLaserBoxDynamicRelative("));
        assertTrue(builder.contains("renderLaserDynamicRelative(pose, normal, data, renderOrigin, bb)"));
        assertFalse(builder.contains("matrix.translate(-pos"));

        String architect = read("src/main/java/buildcraft/builders/client/render/RenderArchitectTable.java");
        assertTrue(architect.contains("Vec3 renderOrigin = Vec3.atLowerCornerOf(pos)"));
        assertTrue(architect.contains("renderLaserBoxDynamicRelative("));
        assertTrue(architect.contains("renderLaserDynamicRelative(pose, normal, data, renderOrigin, bb)"));
        assertFalse(architect.contains("matrix.translate(-pos"));

        String filler = read("src/main/java/buildcraft/builders/client/render/RenderFiller.java");
        assertTrue(filler.contains("Vec3 renderOrigin = Vec3.atLowerCornerOf(pos)"));
        assertTrue(filler.contains("renderLaserBoxDynamicRelative("));
        assertFalse(filler.contains("matrix.translate(-tile.getBlockPos()"));
    }

    @Test
    void templateItemsAreInterpolatedInWorldSpaceThenMadeTileLocal() throws IOException {
        String snapshot = read("src/main/java/buildcraft/builders/client/render/RenderSnapshotBuilder.java");
        assertTrue(snapshot.contains("WorldRenderMath.relative(worldPos, renderOrigin)"));
        assertTrue(snapshot.contains("matrix.translate(localPos.x, localPos.y, localPos.z)"));
        assertTrue(snapshot.contains("matrix.pushPose()"));
        assertTrue(snapshot.contains("matrix.popPose()"));
        assertTrue(snapshot.contains(".orElse(builderStart)"));
        assertTrue(snapshot.contains("LevelRenderer.getLightColor(world, BlockPos.containing(worldPos))"));
        assertFalse(snapshot.contains("new PlaceTask(tilePos, Collections.emptyList(), 0L)"));
        assertFalse(snapshot.contains("matrix.translate(-tilePos"));
    }
    @Test
    void releasedBuilderRobotAndBreakLaserRenderingRemainPresent() throws IOException {
        String builder = read("src/main/java/buildcraft/builders/snapshot/SnapshotBuilder.java");
        assertTrue(builder.contains("public Vec3 robotPos"));
        assertTrue(builder.contains("prevRobotPos = robotPos"));

        String renderer = read("src/main/java/buildcraft/builders/client/render/RenderSnapshotBuilder.java");
        assertTrue(renderer.contains("BCBuildersSprites.ROBOT"));
        assertTrue(renderer.contains("snapshotBuilder.clientBreakTasks"));
        assertTrue(renderer.contains("BuildCraftLaserManager.POWERS[powerIndex]"));
        assertTrue(renderer.contains("renderLaserDynamicRelative(pose, normal, data, renderOrigin, vertexConsumer)"));
    }

    @Test
    void relativeLaserCacheCarriesTheOriginIntoDoublePrecisionVertexBaking() throws IOException {
        String renderer = read("src/main/java/buildcraft/lib/client/render/laser/LaserRenderer_BC8.java");
        assertTrue(renderer.contains("record LaserRenderKey(LaserData_BC8 data, Vec3 renderOrigin)"));
        assertTrue(renderer.contains("key.renderOrigin()"));
        assertTrue(renderer.contains("renderLaserDynamicRelative("));

        String context = read("src/main/java/buildcraft/lib/client/render/laser/LaserContext.java");
        assertTrue(context.contains("WorldRenderMath.relativeAsFloat(data.start.x, originX)"));
        assertTrue(context.contains("WorldRenderMath.relativeAsFloat(data.start.y, originY)"));
        assertTrue(context.contains("WorldRenderMath.relativeAsFloat(data.start.z, originZ)"));
        assertFalse(context.contains("(float) data.start.x - (float) originX"));
    }

    @Test
    void detachedMarkerLasersSubtractTheCameraBeforeFloatConversion() throws IOException {
        String registration = read("src/main/java/buildcraft/lib/BCLibEventDist.java");
        assertTrue(registration.contains("RenderMatrixType.FROM_PLAYER, MarkerRenderer.INSTANCE"));

        String renderer = read("src/main/java/buildcraft/lib/client/render/MarkerRenderer.java");
        assertTrue(renderer.contains("getMainCamera().getPosition()"));
        assertTrue(renderer.contains("renderInWorld(pose, matrix, cameraPosition)"));

        String path = read("src/main/java/buildcraft/core/marker/PathConnection.java");
        assertTrue(path.contains("renderLaserStaticRelative(pose, matrix, data, cameraPosition)"));

        String volume = read("src/main/java/buildcraft/core/marker/VolumeConnection.java");
        assertTrue(volume.contains("renderLaserBoxStaticRelative("));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
