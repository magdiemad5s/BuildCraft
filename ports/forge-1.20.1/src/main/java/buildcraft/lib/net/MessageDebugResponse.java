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

import buildcraft.lib.debug.ClientDebuggables;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class MessageDebugResponse {
    static final int MAX_LINES_PER_COLUMN = 1024;
    static final int MAX_LINE_LENGTH = 4096;

    private final List<String> left = new ArrayList<>();
    private final List<String> right = new ArrayList<>();

    public MessageDebugResponse() {}

    public MessageDebugResponse(List<String> left, List<String> right) {
        this.left.addAll(left);
        this.right.addAll(right);
    }

    public static void toBytes(MessageDebugResponse msg, FriendlyByteBuf buffer) {
        writeColumn(buffer, msg.left);
        writeColumn(buffer, msg.right);
    }

    private static void writeColumn(FriendlyByteBuf buffer, List<String> lines) {
        if (lines.size() > MAX_LINES_PER_COLUMN) {
            throw new EncoderException("Too many debug lines: " + lines.size());
        }
        buffer.writeInt(lines.size());
        for (String line : lines) {
            buffer.writeUtf(line, MAX_LINE_LENGTH);
        }
    }

    public MessageDebugResponse(FriendlyByteBuf buffer) {
        readColumn(buffer, left);
        readColumn(buffer, right);
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing bytes in debug response: " + buffer.readableBytes());
        }
    }

    private static void readColumn(FriendlyByteBuf buffer, List<String> into) {
        int count = buffer.readInt();
        if (count < 0 || count > MAX_LINES_PER_COLUMN) {
            throw new DecoderException("Invalid debug line count: " + count);
        }
        for (int i = 0; i < count; i++) {
            into.add(buffer.readUtf(MAX_LINE_LENGTH));
        }
    }

    public static final BiConsumer<MessageDebugResponse, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientDebuggables.SERVER_LEFT.clear();
            ClientDebuggables.SERVER_LEFT.addAll(message.left);
            ClientDebuggables.SERVER_RIGHT.clear();
            ClientDebuggables.SERVER_RIGHT.addAll(message.right);
        }));
        context.setPacketHandled(true);
    };
}