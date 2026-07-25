/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import buildcraft.api.tiles.IDebuggable;
import io.netty.handler.codec.DecoderException;
import buildcraft.lib.item.ItemDebugger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class MessageDebugRequest {
	private static final double MAX_DISTANCE_SQUARED = 64.0D;
	private BlockPos pos;
	private Direction side;

	@SuppressWarnings("unused")
	public MessageDebugRequest() {
	}

	public MessageDebugRequest(BlockPos pos, Direction side) {
		this.pos = pos;
		this.side = side;
	}

	public static void toBytes(MessageDebugRequest msg, FriendlyByteBuf buffer) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeEnum(msg.side);
	}

	public MessageDebugRequest(FriendlyByteBuf buffer) {
		pos = buffer.readBlockPos();
		side = buffer.readEnum(Direction.class);
		if (buffer.isReadable()) {
			throw new DecoderException("Trailing bytes in debug request: " + buffer.readableBytes());
		}
	}

	public static final BiConsumer<MessageDebugRequest, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
		NetworkEvent.Context context = ctx.get();
		ServerPlayer player = context.getSender();
		if (player == null || context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
			context.setPacketHandled(true);
			return;
		}

		context.enqueueWork(() -> {
			if (!ItemDebugger.isShowDebugInfo(player)) {
				MessageManager.sendTo(new MessageDebugResponse(), player);
				return;
			}
			if (!player.level().isInWorldBounds(message.pos)
					|| !player.level().hasChunkAt(message.pos)
					|| player.distanceToSqr(
						message.pos.getX() + 0.5D,
						message.pos.getY() + 0.5D,
						message.pos.getZ() + 0.5D
					) > MAX_DISTANCE_SQUARED) {
				return;
			}

			BlockEntity tile = player.level().getBlockEntity(message.pos);
			if (tile instanceof IDebuggable debuggable) {
				List<String> left = new ArrayList<>();
				List<String> right = new ArrayList<>();
				debuggable.getDebugInfo(left, right, message.side);
				MessageManager.sendTo(new MessageDebugResponse(left, right), player);
			}
		});
		context.setPacketHandled(true);
	};
}
