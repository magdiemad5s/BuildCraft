/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import buildcraft.api.core.BCLog;
import buildcraft.lib.gui.IMenuBCTile;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import io.netty.handler.codec.DecoderException;

public class MessageUpdateTile {
    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024;

    private BlockPos pos;
    private FriendlyByteBuf payload;

    @SuppressWarnings("unused")
    public MessageUpdateTile() {}

    public MessageUpdateTile(BlockPos pos, FriendlyByteBuf payload) {
        this.pos = pos;
        this.payload = payload;
        if (getPayloadSize() > MAX_PAYLOAD_SIZE) {
            throw new IllegalStateException("Can't write out " + getPayloadSize() + " bytes!");
        }
    }

    public int getPayloadSize() {
        return payload == null ? 0 : payload.readableBytes();
    }

    public MessageUpdateTile(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        int size = buf.readUnsignedMedium();
        if (size > MAX_PAYLOAD_SIZE || size != buf.readableBytes()) {
            throw new DecoderException("Invalid tile update payload size: " + size + " readable=" + buf.readableBytes());
        }
        payload = new FriendlyByteBuf(buf.readBytes(size));
    }

    public static void toBytes(MessageUpdateTile msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        int length = msg.payload.readableBytes();
        if (length > MAX_PAYLOAD_SIZE) {
            throw new IllegalStateException("Tile update payload is too large: " + length);
        }
        buf.writeMedium(length);
        buf.writeBytes(msg.payload, 0, length);
    }

    public static final BiConsumer<MessageUpdateTile, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                LogicalSide receptionSide = context.getDirection().getReceptionSide();
                ServerPlayer sender = context.getSender();
                Level level;
                if (receptionSide == LogicalSide.SERVER) {
                    if (sender == null || sender.level() == null) {
                        BCLog.logger.warn("Dropped serverbound BuildCraft tile update without a live sender");
                        return;
                    }
                    level = sender.level();
                } else {
                    PacketListener netHandler = context.getNetworkManager().getPacketListener();
                    level = MessageUpdateTileClientHandler.getClientLevel(netHandler);
                }
                if (level == null || !level.hasChunkAt(message.pos)) {
                    BCLog.logger.warn("Dropped BuildCraft tile update for unloaded position {}", message.pos);
                    return;
                }

                BlockEntity tile = level.getBlockEntity(message.pos);
                if (!(tile instanceof IPayloadReceiver receiver)) {
                    BCLog.logger.warn("Dropped BuildCraft tile update for {} (found {})", message.pos, tile);
                    return;
                }

                if (receptionSide == LogicalSide.SERVER) {
                    // Every tile payload capable of mutating server state must come from the player actively using that
                    // exact, still-valid BuildCraft menu. This blocks remote arbitrary-position updates and stale GUIs.
                    if (!(tile instanceof TileBC_Neptune buildCraftTile)
                        || !buildCraftTile.canInteractWith(sender)
                        || !(sender.containerMenu instanceof IMenuBCTile menu)
                        || menu.getBCTile() != buildCraftTile
                        || !sender.containerMenu.stillValid(sender)) {
                        BCLog.logger.warn(
                            "Dropped unauthorized BuildCraft tile update from {} for {} at {}",
                            sender.getGameProfile().getName(),
                            tile.getClass().getSimpleName(),
                            message.pos
                        );
                        return;
                    }
                }

                receiver.receivePayload(context, message.payload);
            } catch (IOException | RuntimeException exception) {
                BCLog.logger.warn("Dropped invalid BuildCraft tile update packet", exception);
            } finally {
                message.payload.release();
            }
        });
        context.setPacketHandled(true);
    };
}
