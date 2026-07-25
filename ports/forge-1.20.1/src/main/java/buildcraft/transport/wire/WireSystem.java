/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.IWireEmitter;
import buildcraft.api.transport.WireNode;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.transport.pipe.Pipe;
import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.DecoderException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WireSystem {
    private static final int MAX_SAVED_ELEMENTS = 16384;
    private static final AtomicInteger NETWORK_IDS = new AtomicInteger();

    private static int nextServerNetworkId() {
        return NETWORK_IDS.incrementAndGet();
    }

    public final ImmutableList<WireElement> elements;
    public final DyeColor color;
    /** Runtime-only packet identity. Deliberately unrelated to hashCode, which can collide. */
    public final int networkId;

    private transient final int cachedHashCode;
    private transient final int cachedWiresHashCode;

    public boolean hasElement(WireElement element) {
        return elements.contains(element);
    }

    /** Checks to see if the given holder could connect a wire across the specified side even if a matching wire wasn't
     * there. */
    public static boolean canWireConnect(IPipeHolder holder, Direction side) {
        // TODO: Expand to pipeless wires (8.1.x)
        IPipe pipe = holder.getPipe();
        if (pipe == Pipe.EMPTY) {
            return false;
        }
        IPipe oPipe = holder.getNeighbourPipe(side);
        if (oPipe == Pipe.EMPTY) {
            return false;
        }
        if (pipe.isConnected(side)) {
            return true;
        }
        if ((holder.getPluggable(side) != null && holder.getPluggable(side).isBlocking()) //
            || (oPipe.getHolder().getPluggable(side.getOpposite()) != null && oPipe.getHolder().getPluggable(side.getOpposite()).isBlocking())) {
            return false;
        }
        if (pipe.getDefinition().flowType == PipeApi.flowStructure || oPipe.getDefinition().flowType == PipeApi.flowStructure) {
            return pipe.getColour() == null || oPipe.getColour() == null || pipe.getColour() == oPipe.getColour();
        }
        return false;
    }

    public static List<WireElement> getConnectedElementsOfElement(IPipeHolder holder, WireElement element) {
        assert element.wirePart != null;
        WireNode node = new WireNode(element.blockPos, element.wirePart);

        List<WireElement> list = new ArrayList<>();
        for (Direction face : Direction.values()) {
            WireNode oNode = node.offset(face);
            // equality check is fine here -- WireNode.offset returns the same blockpos (identity wise) if its the same
            if (oNode.pos == node.pos || canWireConnect(holder, face)) {
                list.add(new WireElement(oNode.pos, oNode.part));
            }
        }
        return list;
    }

    public static List<WireElement> getConnectedElementsOfElement(Level world, WireElement element) {
        if (element.type == WireElement.Type.WIRE_PART) {
            BlockEntity tile = world.getBlockEntity(element.blockPos);
            if (tile instanceof IPipeHolder) {
                IPipeHolder holder = (IPipeHolder) tile;
                return getConnectedElementsOfElement(holder, element);
            }
        }
        return Collections.emptyList();
    }

    public WireSystem(ImmutableList<WireElement> elements, DyeColor color) {
        this(nextServerNetworkId(), elements, color);
    }

    /** Client/network constructor: preserves the server-assigned packet identity. */
    public WireSystem(int networkId, ImmutableList<WireElement> elements, DyeColor color) {
        this.networkId = networkId;
        this.elements = Objects.requireNonNull(elements, "elements");
        this.color = color;

        this.cachedHashCode = this.computeHashCode();
        this.cachedWiresHashCode = this.computeCachedWiresHashCode();
    }

    public WireSystem(WorldSavedDataWireSystems wireSystems, WireElement startElement) {
        this.networkId = nextServerNetworkId();
        long time = System.currentTimeMillis();
        Map<BlockPos, IPipeHolder> holdersCache = new HashMap<>();
        Set<WireElement> walked = new HashSet<>();

        Queue<WireElement> queue = new ArrayDeque<>();
        queue.add(startElement);

        DyeColor tempColor = null;
        ImmutableList.Builder<WireElement> elementBuilder = ImmutableList.builder();

        while (!queue.isEmpty()) {
            WireElement element = queue.remove();

            if (!walked.contains(element)) {
                if (!holdersCache.containsKey(element.blockPos)) {
                    BlockEntity tile = wireSystems.world.getBlockEntity(element.blockPos);
                    IPipeHolder holder = null;
                    if (tile instanceof IPipeHolder) {
                        holder = (IPipeHolder) tile;
                    }
                    holdersCache.put(element.blockPos, holder);
                }
                IPipeHolder holder = holdersCache.get(element.blockPos);
                if (holder != null) {
                    if (element.type == WireElement.Type.WIRE_PART) {
                        DyeColor colorOfPart = holder.getWireManager().getColorOfPart(element.wirePart);
                        if (tempColor == null) {
                            if (colorOfPart != null) {
                                tempColor = colorOfPart;
                            }
                        }
                        if (tempColor != null && colorOfPart == tempColor) {
                            DyeColor colorButFinal = tempColor; //damn you java
                            wireSystems.getWireSystemsWithElement(element).stream().filter(wireSystem -> wireSystem != this && wireSystem.color == colorButFinal).forEach(wireSystems::removeWireSystem);
                            elementBuilder.add(element);
                            queue.addAll(getConnectedElementsOfElement(wireSystems.world, element));
                            Arrays.stream(Direction.values()).forEach(side -> queue.add(new WireElement(element.blockPos, side)));
                        }
                    } else if (element.type == WireElement.Type.EMITTER_SIDE) {
                        if (holder.getPluggable(element.emitterSide) instanceof IWireEmitter) {
                            elementBuilder.add(new WireElement(element.blockPos, element.emitterSide));
                        }
                    }
                }
                walked.add(element);
            }
        }

        this.elements = elementBuilder.build();
        this.color = tempColor;

        this.cachedHashCode = this.computeHashCode();
        this.cachedWiresHashCode = this.computeCachedWiresHashCode();
    }

    public boolean isEmpty() {
        return elements.stream().filter(element -> element.type == WireElement.Type.WIRE_PART).count() == 0;
    }

    public boolean update(WorldSavedDataWireSystems wireSystems) {
        return elements.stream().filter(element -> element.type == WireElement.Type.EMITTER_SIDE).map(element -> wireSystems.isEmitterEmitting(element, color)).reduce(Boolean::logicalOr).orElse(
            false);
    }

    public List<ChunkPos> getChunkPoses() {
        return this.getChunkPosesAsStream().collect(Collectors.toList());
    }

    public Stream<ChunkPos> getChunkPosesAsStream() {
        return elements.stream().map(element -> new ChunkPos(element.blockPos));
    }
    

    public boolean isPlayerWatching(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return elements.stream()
            .map(element -> new ChunkPos(element.blockPos))
            .anyMatch(chunkPos -> serverPlayer.serverLevel().getChunkSource().chunkMap
                .getPlayers(chunkPos, false).contains(serverPlayer));
    }

    public int getWiresHashCode() {
        return this.cachedWiresHashCode;
    }

    private int computeCachedWiresHashCode() {
        return elements.stream().filter(element -> element.type == WireElement.Type.WIRE_PART)
                //the following is equivalent to .collect(Collectors.toList()).hashCode(), by the definition of List#hashCode():
                .mapToInt(WireElement::hashCode).reduce(1, (hashCode, elementHashCode) -> hashCode * 31 + elementHashCode);
    }

    public CompoundTag writeToNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag elementsList = new ListTag();
        for (WireElement element : elements) {
            elementsList.add(element.writeToNBT());
        }
        nbt.put("elements", elementsList);
        nbt.putInt("color", color.getId());
        return nbt;
    }

    public WireSystem(CompoundTag nbt) {
        networkId = nextServerNetworkId();
        int colourId = nbt.getInt("color");
        if (colourId < 0 || colourId >= DyeColor.values().length) {
            throw new IllegalArgumentException("Invalid wire system colour " + colourId);
        }
        color = DyeColor.byId(colourId);

        ListTag elementsList = nbt.getList("elements", Tag.TAG_COMPOUND);
        if (elementsList.size() > MAX_SAVED_ELEMENTS) {
            throw new IllegalArgumentException("Too many saved wire elements: " + elementsList.size());
        }
        ImmutableList.Builder<WireElement> builder = ImmutableList.builder();
        for (int index = 0; index < elementsList.size(); index++) {
            builder.add(new WireElement(elementsList.getCompound(index)));
        }
        elements = builder.build();
        if (elements.stream().noneMatch(element -> element.type == WireElement.Type.WIRE_PART)) {
            throw new IllegalArgumentException("Saved wire system contains no wire parts");
        }

        this.cachedHashCode = this.computeHashCode();
        this.cachedWiresHashCode = this.computeCachedWiresHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WireSystem that = (WireSystem) o;

        if (this.cachedHashCode != that.cachedHashCode) {
            //both have a cached hashCode, and the hash codes don't match
            return false;
        }

        if (!elements.equals(that.elements)) {
            return false;
        }
        return color == that.color;
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

    private int computeHashCode() {
        int result = elements.hashCode();
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }

    public static class WireElement {
        public final Type type;
        public final BlockPos blockPos;
        public final EnumWirePart wirePart;
        public final Direction emitterSide;

        public WireElement(BlockPos blockPos, EnumWirePart wirePart) {
            this.type = Type.WIRE_PART;
            this.blockPos = blockPos;
            this.wirePart = wirePart;
            this.emitterSide = null;
        }

        public WireElement(BlockPos blockPos, Direction emitterSide) {
            this.type = Type.EMITTER_SIDE;
            this.blockPos = blockPos;
            this.wirePart = null;
            this.emitterSide = emitterSide;
        }

        public WireElement(FriendlyByteBuf buf) {
            int typeOrdinal = buf.readInt();
            if (typeOrdinal < 0 || typeOrdinal >= Type.values().length) {
                throw new DecoderException("Invalid wire element type " + typeOrdinal);
            }
            type = Type.values()[typeOrdinal];
            blockPos = MessageUtil.readBlockPos(buf);
            if (type == Type.WIRE_PART) {
                int partOrdinal = buf.readInt();
                if (partOrdinal < 0 || partOrdinal >= EnumWirePart.VALUES.length) {
                    throw new DecoderException("Invalid wire part " + partOrdinal);
                }
                wirePart = EnumWirePart.VALUES[partOrdinal];
                this.emitterSide = null;
            } else {
                this.wirePart = null;
                int sideOrdinal = buf.readInt();
                if (sideOrdinal < 0 || sideOrdinal >= Direction.values().length) {
                    throw new DecoderException("Invalid wire emitter side " + sideOrdinal);
                }
                emitterSide = Direction.from3DDataValue(sideOrdinal);
            }
        }

        public WireElement(CompoundTag nbt) {
            int typeOrdinal = nbt.getInt("type");
            if (typeOrdinal < 0 || typeOrdinal >= Type.values().length) {
                throw new IllegalArgumentException("Invalid saved wire element type " + typeOrdinal);
            }
            type = Type.values()[typeOrdinal];
            blockPos = NBTUtilBC.readBlockPos(nbt.getIntArray("blockPos"));
            if (blockPos == null) {
                throw new IllegalArgumentException("Invalid saved wire element position");
            }
            if (type == Type.WIRE_PART) {
                int partOrdinal = nbt.getInt("wirePart");
                if (partOrdinal < 0 || partOrdinal >= EnumWirePart.VALUES.length) {
                    throw new IllegalArgumentException("Invalid saved wire part " + partOrdinal);
                }
                wirePart = EnumWirePart.VALUES[partOrdinal];
                this.emitterSide = null;
            } else {
                this.wirePart = null;
                int sideOrdinal = nbt.getInt("emitterSide");
                if (sideOrdinal < 0 || sideOrdinal >= Direction.values().length) {
                    throw new IllegalArgumentException("Invalid saved wire emitter side " + sideOrdinal);
                }
                emitterSide = Direction.from3DDataValue(sideOrdinal);
            }
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeInt(type.ordinal());
            MessageUtil.writeBlockPos(buf, blockPos);
            if (type == Type.WIRE_PART) {
                assert wirePart != null;
                buf.writeInt(wirePart.ordinal());
            } else if (type == Type.EMITTER_SIDE) {
                assert emitterSide != null;
                buf.writeInt(emitterSide.get3DDataValue());
            }
        }

        public CompoundTag writeToNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("type", type.ordinal());
            nbt.putIntArray("blockPos", NBTUtilBC.writeBlockPos(blockPos));
            if (type == Type.WIRE_PART) {
                assert wirePart != null;
                nbt.putInt("wirePart", wirePart.ordinal());
            } else if (type == Type.EMITTER_SIDE) {
                assert emitterSide != null;
                nbt.putInt("emitterSide", emitterSide.get3DDataValue());
            }
            return nbt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WireElement element = (WireElement) o;

            if (type != element.type) {
                return false;
            }
            if (!blockPos.equals(element.blockPos)) {
                return false;
            }
            if (wirePart != element.wirePart) {
                return false;
            }
            return emitterSide == element.emitterSide;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + blockPos.hashCode();
            result = 31 * result + (wirePart != null ? wirePart.hashCode() : 0);
            result = 31 * result + (emitterSide != null ? emitterSide.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Element{" + "type=" + type + ", blockPos=" + blockPos + ", wirePart=" + wirePart + ", emitterSide=" + emitterSide + '}';
        }

        public enum Type {
            WIRE_PART,
            EMITTER_SIDE
        }
    }
}
