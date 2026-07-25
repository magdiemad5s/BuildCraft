/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.transport.wire;

import java.util.Collection;
import java.util.function.Supplier;

import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.pipe.IPipeHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

/** Replaces the complete watched-wire snapshot and removes power left behind by retired systems. */
@OnlyIn(Dist.CLIENT)
public final class MessageWireSystemsClientHandler {
    private MessageWireSystemsClientHandler() {
    }

    public static void handle(MessageWireSystems message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                clearPoweredState(level, ClientWireSystems.INSTANCE.wireSystems.values());
            }
            ClientWireSystems.INSTANCE.wireSystems.clear();
            ClientWireSystems.INSTANCE.wireSystems.putAll(message.wireSystems);
        });
    }

    private static void clearPoweredState(Level level, Collection<WireSystem> systems) {
        for (WireSystem system : systems) {
            for (WireSystem.WireElement element : system.elements) {
                if (element.type != WireSystem.WireElement.Type.WIRE_PART) {
                    continue;
                }
                BlockEntity blockEntity = level.getBlockEntity(element.blockPos);
                if (blockEntity instanceof IPipeHolder holder) {
                    IWireManager manager = holder.getWireManager();
                    if (manager instanceof WireManager wireManager
                            && wireManager.poweredClient.remove(element.wirePart)) {
                        holder.scheduleRenderUpdate();
                    }
                }
            }
        }
    }
}
