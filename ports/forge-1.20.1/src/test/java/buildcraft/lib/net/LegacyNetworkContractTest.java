/* Copyright (c) 2026 BuildCraft Neo contributors
 * SPDX-License-Identifier: MPL-2.0 */
package buildcraft.lib.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import buildcraft.lib.net.cache.MessageObjectCacheRequest;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;

class LegacyNetworkContractTest {
    @Test
    void legacyLibraryDiscriminatorsRemainLexicographic() {
        List<Class<?>> classes = new ArrayList<>(List.of(
            MessageUpdateTile.class, MessageObjectCacheResponse.class, MessageMarker.class,
            MessageDebugRequest.class, MessageContainer.class, MessageObjectCacheRequest.class,
            MessageDebugResponse.class));
        classes.sort(MessageManager.MESSAGE_CLASS_ORDER);
        assertEquals(List.of(MessageContainer.class, MessageDebugRequest.class,
            MessageDebugResponse.class, MessageMarker.class, MessageUpdateTile.class,
            MessageObjectCacheRequest.class, MessageObjectCacheResponse.class), classes);
    }

    @Test
    void singleSidedHandlersConstrainForgePacketDirection() {
        assertEquals(Optional.of(NetworkDirection.PLAY_TO_CLIENT), MessageManager.expectedDirection(true, false));
        assertEquals(Optional.of(NetworkDirection.PLAY_TO_SERVER), MessageManager.expectedDirection(false, true));
        assertEquals(Optional.empty(), MessageManager.expectedDirection(true, true));
    }

    @Test
    void boundedCodecsRejectHostileCounts() {
        FriendlyByteBuf marker = new FriendlyByteBuf(Unpooled.buffer());
        marker.writeBoolean(false).writeBoolean(true).writeBoolean(false).writeShort(0).writeShort(-1);
        assertThrows(DecoderException.class, () -> new MessageMarker(marker));
        marker.release();

        FriendlyByteBuf debug = new FriendlyByteBuf(Unpooled.buffer());
        debug.writeInt(MessageDebugResponse.MAX_LINES_PER_COLUMN + 1);
        assertThrows(DecoderException.class, () -> new MessageDebugResponse(debug));
        debug.release();
    }
}