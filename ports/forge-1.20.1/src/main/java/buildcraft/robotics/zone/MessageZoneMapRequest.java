/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import buildcraft.lib.misc.MessageUtil;
import buildcraft.robotics.container.ContainerZonePlanner;
import net.minecraft.core.BlockPos;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class MessageZoneMapRequest {
    private static final ZoneMapRequestPolicy.RateLimiter RATE_LIMITER =
        new ZoneMapRequestPolicy.RateLimiter();

    private ZonePlannerMapChunkKey key;

    @SuppressWarnings("unused")
    public MessageZoneMapRequest() {
    }

    public MessageZoneMapRequest(ZonePlannerMapChunkKey key) {
        this.key = key;
    }

    public MessageZoneMapRequest(FriendlyByteBuf buf) {
        key = new ZonePlannerMapChunkKey(buf);
        if (buf.isReadable()) {
            throw new DecoderException("Trailing bytes in zone map request: " + buf.readableBytes());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        key.toBytes(buf);
    }

    public static final BiConsumer<MessageZoneMapRequest, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide() != LogicalSide.SERVER) {
            context.setPacketHandled(true);
            return;
        }
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender == null
                    || !(sender.containerMenu instanceof ContainerZonePlanner menu)
                    || menu.tile == null
                    || !menu.stillValid(sender)
                    || message.key == null) {
                return;
            }

            ServerLevel level = sender.serverLevel();
            ZonePlannerMapChunkKey key = message.key;
            int expectedDimension = level.dimension().location().hashCode();
            BlockPos plannerPos = menu.tile.getBlockPos();
            if (!ZoneMapRequestPolicy.isExpectedDimension(key.dimensionalId, expectedDimension)
                    || !ZoneMapRequestPolicy.isValidLevel(
                        key.level,
                        level.getMinBuildHeight(),
                        level.getMaxBuildHeight()
                    )
                    || !ZoneMapRequestPolicy.isWithinMapRadius(
                        plannerPos.getX(),
                        plannerPos.getZ(),
                        key.chunkPos.x,
                        key.chunkPos.z
                    )
                    || !RATE_LIMITER.allow(sender.getUUID(), level.getGameTime())) {
                return;
            }

            BlockPos samplePos = new BlockPos(
                key.chunkPos.getMinBlockX() + 8,
                plannerPos.getY(),
                key.chunkPos.getMinBlockZ() + 8
            );
            ZonePlannerMapChunk data = level.hasChunkAt(samplePos)
                ? ZonePlannerMapDataServer.INSTANCE.getChunk(level, key)
                : new ZonePlannerMapChunk();
            MessageUtil.sendReturnMessage(context, new MessageZoneMapResponse(key, data));
        });
        context.setPacketHandled(true);
    };
}
