/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import buildcraft.api.IBuildCraftMod;
import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MessageManager {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.messages");

    static final Comparator<Class<?>> MESSAGE_CLASS_ORDER = Comparator.comparing(Class::getName);
    private static final Map<IBuildCraftMod, PerModHandler> MOD_HANDLERS;
    private static final Map<Class<?>, PerMessageInfo<?>> MESSAGE_HANDLERS = new ConcurrentHashMap<>();
    public static final String PROTOCOL_VERSION = "BC8.0.x-1.20.1";

    static {
        MOD_HANDLERS = new TreeMap<>(MessageManager::compareMods);
    }

    
    
    
    
    
    
    static class PerModHandler {
        final IBuildCraftMod module;
        final SimpleChannel netWrapper;
        final SortedMap<Class<?>, PerMessageInfo<?>> knownMessages;

        PerModHandler(IBuildCraftMod module) {
            this.module = module;
            this.netWrapper = NetworkRegistry.newSimpleChannel(new ResourceLocation(module.getModId(),"channel"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
            BCLog.logger.debug("registing Channel for " + module.getModId());
            knownMessages = new TreeMap<>(MESSAGE_CLASS_ORDER);
        }
    }

    static class PerMessageInfo<I> {
        final PerModHandler modHandler;
        final Class<I> messageClass;
        final BiConsumer<I, FriendlyByteBuf> enCoder;
        final Function<FriendlyByteBuf, I> deCoder;

        /** The handler to register, or null if this isn't handled in this physical side. */
        @Nullable
        BiConsumer<I, Supplier<NetworkEvent.Context>> clientHandler, serverHandler;

        PerMessageInfo(PerModHandler modHandler, Class<I> messageClass, BiConsumer<I, FriendlyByteBuf> enCoder, Function<FriendlyByteBuf, I> deCoder) {
            this.modHandler = modHandler;
            this.messageClass = messageClass;
            this.enCoder = enCoder;
            this.deCoder = deCoder;
        }
    }

    private static int compareMods(IBuildCraftMod modA, IBuildCraftMod modB) {
        if (modA instanceof Enum && modB instanceof Enum) {
            Enum<?> enumA = (Enum<?>) modA;
            Enum<?> enumB = (Enum<?>) modB;
            if (enumA.getDeclaringClass() == enumB.getDeclaringClass()) {
                return Integer.compare(enumA.ordinal(), enumB.ordinal());
            }
        }
        return modA.getModId().compareTo(modB.getModId());
    }

    /** Registers a message as one that will not be received, but will be sent. */
    public static <I> void registerMessageClass(IBuildCraftMod module, Class<I> clazz, BiConsumer<I, FriendlyByteBuf> enCoder, Function<FriendlyByteBuf, I> deCoder, Dist... sides) {
        registerMessageClass(module, clazz, null, enCoder, deCoder, sides);
    }
    

    public static synchronized <I> void registerMessageClass(IBuildCraftMod module, Class<I> messageClass,
    	BiConsumer<I, Supplier<NetworkEvent.Context>> messageHandler,
    	BiConsumer<I, FriendlyByteBuf> enCoder,
    	Function<FriendlyByteBuf, I> deCoder, Dist... sides) {
        //PerModHandler modHandler = MOD_HANDLERS.computeIfAbsent(module, PerModHandler::new);
    	PerModHandler modHandler;
    	synchronized(MOD_HANDLERS) {
	    	if(!MOD_HANDLERS.containsKey(module)) {
	    		modHandler = new PerModHandler(module);
	    		MOD_HANDLERS.put(module, modHandler);
	    	}
	    	else {
				modHandler = MOD_HANDLERS.get(module);
			}
    	}
        PerMessageInfo<I> messageInfo = (PerMessageInfo<I>) modHandler.knownMessages.get(messageClass);
        if (messageInfo == null) {
            PerMessageInfo<?> existing = MESSAGE_HANDLERS.get(messageClass);
            if (existing != null && existing.modHandler != modHandler) {
                throw new IllegalArgumentException(
                    "Message " + messageClass.getName() + " is already registered on "
                        + existing.modHandler.module.getModId()
                );
            }
            messageInfo = new PerMessageInfo<>(modHandler, messageClass, enCoder, deCoder);
            modHandler.knownMessages.put(messageClass, messageInfo);
            MESSAGE_HANDLERS.put(messageClass, messageInfo);
        }
        String netName = module.getModId();
        if (messageHandler == null) {
            if (DEBUG) {
                BCLog.logger.info("[lib.messages] Registered message " + messageClass + " for " + netName);
            }
            return;
        }
        Dist specificSide = sides != null && sides.length == 1 ? sides[0] : null;
        if (specificSide == null || specificSide == Dist.CLIENT) {
            if (messageInfo.clientHandler != null && DEBUG) {
                BCLog.logger.info("[lib.messages] Replacing existing client handler for " + netName + " " + messageClass
                    + " " + messageInfo.clientHandler + " with " + messageHandler);
            }
            messageInfo.clientHandler = messageHandler;
        }
        if (specificSide == null || specificSide == Dist.DEDICATED_SERVER) {
            if (messageInfo.serverHandler != null && DEBUG) {
                BCLog.logger.info("[lib.messages] Replacing existing server handler for " + netName + " " + messageClass
                    + " " + messageInfo.serverHandler + " with " + messageHandler);
            }
            messageInfo.serverHandler = messageHandler;
        }
    }

    /** Sets the handler for the specified handler.
     * 
     * @param side The side that the given handler will receive messages on. */
    public static <I> void setHandler(Class<I> messageClass,
    	BiConsumer<I, Supplier<NetworkEvent.Context>> messageHandler, Dist side) {
        PerMessageInfo<I> messageInfo = (PerMessageInfo<I>) MESSAGE_HANDLERS.get(messageClass);
        if (messageInfo == null) {
            throw new IllegalArgumentException("Cannot set handler for unregistered message: " + messageClass);
        }
        
        registerMessageClass(messageInfo.modHandler.module, messageClass, messageHandler, messageInfo.enCoder, messageInfo.deCoder, side);
    }

    /** Called by {@link BCLib} to finish registering this class. */
    public static synchronized void fmlPostInit() {
        if (DEBUG) {
            BCLog.logger.info("[lib.messages] Sorting and registering message classes and orders:");
        }
        for (PerModHandler handler : MOD_HANDLERS.values()) {
            if (DEBUG) {
                BCLog.logger.info("[lib.messages]  - Module: " + handler.module.getModId());
            }
            int messageId = 0;
            for (PerMessageInfo<?> info : handler.knownMessages.values()) {
                postInitSingle(handler, messageId++, info);
            }
        }
    }

    private static <I> void postInitSingle(PerModHandler handler, int messageId, PerMessageInfo<I> info) {
        boolean cl = info.clientHandler != null;
        boolean sv = info.serverHandler != null;
        if (!(cl | sv) && FMLEnvironment.dist == Dist.CLIENT) {
            // The physical client must be able to receive every registered message class.
            throw new IllegalStateException("Found a registered message " + info.messageClass + " for "
                + info.modHandler.module.getModId() + " that didn't have any handlers!");
        }

        Class<I> msgClass = info.messageClass;
        // Forge 1.12 registered the same discriminator once per reception side. SimpleChannel maps a message class to
        // one discriminator, so register it once and dispatch by NetworkDirection inside the wrapper. This preserves
        // the legacy per-module, alphabetically sorted packet IDs without duplicate-class registration races.
        handler.netWrapper.registerMessage(
            messageId,
            msgClass,
            info.enCoder,
            info.deCoder,
            wrapHandler(info, msgClass),
            expectedDirection(cl, sv)
        );
        BCLog.logger.debug("Registered message {}:{} for {}", handler.module.getModId(), messageId, msgClass.getSimpleName());
        if (DEBUG) {
            String sides = cl ? (sv ? "{client, server}" : "{client}") : "{server}";
            BCLog.logger.info("[lib.messages]      " + messageId + ": " + msgClass + " on sides: " + sides);
        }
    }

    static Optional<NetworkDirection> expectedDirection(boolean hasClientHandler, boolean hasServerHandler) {
        if (hasClientHandler == hasServerHandler) {
            return Optional.empty();
        }
        return Optional.of(
            hasClientHandler ? NetworkDirection.PLAY_TO_CLIENT : NetworkDirection.PLAY_TO_SERVER
        );
    }

    private static <I> BiConsumer<I, Supplier<NetworkEvent.Context>> wrapHandler(
        PerMessageInfo<I> info,
        Class<I> messageClass
    ) {
        return (message, contextSupplier) -> {
            NetworkEvent.Context context = contextSupplier.get();
            NetworkDirection direction = context.getDirection();
            final boolean toClient;
            if (direction == NetworkDirection.PLAY_TO_CLIENT) {
                toClient = true;
            } else if (direction == NetworkDirection.PLAY_TO_SERVER) {
                toClient = false;
            } else {
                BCLog.logger.warn("[lib.messages] Dropped {} on unsupported direction {}", messageClass, direction);
                context.setPacketHandled(true);
                return;
            }

            BiConsumer<I, Supplier<NetworkEvent.Context>> messageHandler =
                toClient ? info.clientHandler : info.serverHandler;
            if (messageHandler == null) {
                if (toClient) {
                    BCLog.logger.error(
                        "Received message {} on the client, when it should only be sent by the client and received on the server!",
                        messageClass
                    );
                } else {
                    ServerPlayer player = context.getSender();
                    if (player == null) {
                        BCLog.logger.warn("[lib.messages] Dropped invalid serverbound message {} without a sender", messageClass);
                    } else {
                        BCLog.logger.warn(
                            "[lib.messages] The client {} (ID = {}) sent invalid message {}, when it should only receive it!",
                            player.getName().getString(),
                            player.getGameProfile().getId(),
                            messageClass
                        );
                    }
                }
                context.setPacketHandled(true);
                return;
            }

            if (toClient) {
                // Clientbound packets correctly have no ServerPlayer sender. Gate only on the physical distribution.
                context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> messageHandler.accept(message, contextSupplier)
                ));
            } else {
                ServerPlayer player = context.getSender();
                if (player == null || player.level() == null) {
                    BCLog.logger.warn("[lib.messages] Dropped serverbound message {} without a live sender", messageClass);
                    context.setPacketHandled(true);
                    return;
                }
                context.enqueueWork(() -> messageHandler.accept(message, contextSupplier));
            }
            context.setPacketHandled(true);
        };
    }
    private static SimpleChannel getSimpleNetworkWrapper(Object message) {
        PerMessageInfo<?> info = MESSAGE_HANDLERS.get(message.getClass());
        if (info == null) {
            throw new IllegalArgumentException("Cannot send unregistered message " + message.getClass());
        }
        return info.modHandler.netWrapper;
    }

    /** Send this message to everyone. The {@link MessageHandler} for this message type should be on the CLIENT side.
     *
     * @param message The message to send */
    public static void sendToAll(Object message) {
        getSimpleNetworkWrapper(message).send(PacketDistributor.ALL.noArg(), message);
    }

    /** Send this message to the specified player. The {@link MessageHandler} for this message type should be on the
     * CLIENT side.
     *
     * @param message The message to send
     * @param player The player to send it to */
    public static void sendTo(Object message, ServerPlayer player) {
//        getSimpleNetworkWrapper(message).sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
	    	getSimpleNetworkWrapper(message).send(PacketDistributor.PLAYER.with(() -> player), message);
    }


    /** Send this message to the server. The {@link MessageHandler} for this message type should be on the SERVER side.
     *
     * @param message The message to send */
    public static void sendToServer(Object message) {
        getSimpleNetworkWrapper(message).sendToServer(message);
    }
    
    public static void sendToAllWatching(Object message, LevelChunk levelChunk) {
	    	getSimpleNetworkWrapper(message).send(PacketDistributor.TRACKING_CHUNK.with(() -> levelChunk), message);
    }

	public static void sendToDimension(Object message, ResourceKey<Level> dimensionId) {
		getSimpleNetworkWrapper(message).send(PacketDistributor.DIMENSION.with(() -> dimensionId), message);
	}
}
