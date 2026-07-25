/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class MessageWireSystems {
    private static final int MAX_SYSTEMS = 4096;
    private static final int MAX_ELEMENTS_PER_SYSTEM = 16384;

    Map<Integer, WireSystem> wireSystems = new HashMap<>();

    @SuppressWarnings("unused")
    public MessageWireSystems() {
    }

    public MessageWireSystems(Map<Integer, WireSystem> wireSystems) {
        this.wireSystems = wireSystems;
    }

    public static void toBytes(MessageWireSystems msg, FriendlyByteBuf buf) {
        FriendlyByteBuf pb = new FriendlyByteBuf(buf);
        if (msg.wireSystems.size() > MAX_SYSTEMS) {
            throw new IllegalStateException("Too many wire systems: " + msg.wireSystems.size());
        }
        pb.writeInt(msg.wireSystems.size());
        msg.wireSystems.forEach((networkId, wireSystem) -> {
            pb.writeInt(networkId);
            List<WireSystem.WireElement> elements = wireSystem.elements.stream()
                    .filter(element -> element.type == WireSystem.WireElement.Type.WIRE_PART)
                    .limit(MAX_ELEMENTS_PER_SYSTEM)
                    .collect(Collectors.toList());
            pb.writeInt(elements.size());
            elements.forEach(element -> element.toBytes(pb));
        });
    }

    public MessageWireSystems(FriendlyByteBuf buf) {
        FriendlyByteBuf pb = new FriendlyByteBuf(buf);
        wireSystems.clear();
        int count = pb.readInt();
        if (count < 0 || count > MAX_SYSTEMS) {
            throw new DecoderException("Invalid wire system count: " + count);
        }
        for (int i = 0; i < count; i++) {
            int networkId = pb.readInt();
            int localCount = pb.readInt();
            if (localCount < 0 || localCount > MAX_ELEMENTS_PER_SYSTEM) {
                throw new DecoderException("Invalid wire element count: " + localCount);
            }

            ImmutableList.Builder<WireSystem.WireElement> elements = ImmutableList.builder();
            for (int j = 0; j < localCount; j++) {
                elements.add(new WireSystem.WireElement(pb));
            }
            WireSystem wireSystem = new WireSystem(networkId, elements.build(), null);

            if (wireSystems.put(networkId, wireSystem) != null) {
                throw new DecoderException("Duplicate wire network id: " + networkId);
            }
        }
    }

    public static final BiConsumer<MessageWireSystems, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MessageWireSystemsClientHandler.handle(message, ctx));
        ctx.get().setPacketHandled(true);
    };
}
