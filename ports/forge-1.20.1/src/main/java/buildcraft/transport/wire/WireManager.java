/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.transport.pipe.Pipe;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class WireManager implements IWireManager {
    private static final int MAX_PARTS = EnumWirePart.VALUES.length;
    private final IPipeHolder holder;
    public final Map<EnumWirePart, DyeColor> parts = new EnumMap<>(EnumWirePart.class);
    public final Set<EnumWirePart> poweredClient = EnumSet.noneOf(EnumWirePart.class);
    public final Map<EnumWireBetween, DyeColor> betweens = new EnumMap<>(EnumWireBetween.class);
    public boolean initialised = false;
    // TODO: Wire connections to adjacent blocks

    public WireManager(IPipeHolder holder) {
        this.holder = holder;
    }

    public WorldSavedDataWireSystems getWireSystems() {
        return WorldSavedDataWireSystems.get(holder.getPipeWorld());
    }

    @Override
    public IPipeHolder getHolder() {
        return holder;
    }

    public void invalidate() {
        if (!holder.getPipeWorld().isClientSide()&&initialised) {
            removePartsFromSystem(parts.keySet());
            initialised = false;
        }
    }

    public void validate() {
        if (!holder.getPipeWorld().isClientSide()) {
            initialised = false;
        }
    }

    public void tick() {
        if (!initialised) {
            initialised = true;
            if (!holder.getPipeWorld().isClientSide()) {
                for (EnumWirePart part : parts.keySet()) {
                    getWireSystems().buildAndAddWireSystem(new WireSystem.WireElement(holder.getPipePos(), part));
                }
            }
            updateBetweens(false);
        }
    }

    @Override
    public boolean addPart(EnumWirePart part, DyeColor colour) {
        if (getColorOfPart(part) == null) {
            parts.put(part, colour);
            if (!holder.getPipeWorld().isClientSide()) {
                getWireSystems().buildAndAddWireSystem(new WireSystem.WireElement(holder.getPipePos(), part));
                holder.getPipeTile().setChanged();
            }
            updateBetweens(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public DyeColor removePart(EnumWirePart part) {
        DyeColor color = getColorOfPart(part);
        if (color == null) {
            return null;
        } else {
            parts.remove(part);
            if (!holder.getPipeWorld().isClientSide()) {
                WireSystem.WireElement element = new WireSystem.WireElement(holder.getPipePos(), part);
                WireSystem.getConnectedElementsOfElement(holder, element)
                    .forEach(getWireSystems()::buildAndAddWireSystem);
                getWireSystems().getWireSystemsWithElement(element).forEach(getWireSystems()::removeWireSystem);
                holder.getPipeTile().setChanged();
            }
            updateBetweens(false);
            return color;
        }
    }

    public void removeParts(Collection<EnumWirePart> toRemove) {
        boolean changed = false;
        for (EnumWirePart part : toRemove) {
            changed |= parts.remove(part) != null;
        }
        if (!changed) {
            return;
        }
        if (!holder.getPipeWorld().isClientSide()) {
            removePartsFromSystem(toRemove);
            BlockEntity tile = holder.getPipeTile();
            if (tile != null) {
                tile.setChanged();
            }
        }
        updateBetweens(false);
    }

    private void removePartsFromSystem(Collection<EnumWirePart> toRemove) {
        toRemove.stream().map(part -> new WireSystem.WireElement(holder.getPipePos(), part))
            .flatMap(element -> WireSystem.getConnectedElementsOfElement(holder, element).stream()).distinct()
            .forEach(getWireSystems()::buildAndAddWireSystem);
        toRemove.stream().map(part -> new WireSystem.WireElement(holder.getPipePos(), part))
            .flatMap(element -> getWireSystems().getWireSystemsWithElement(element).stream())
            .forEach(getWireSystems()::removeWireSystem);
//        holder.getPipeTile().setChanged();
    }

    public void rotate(Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return;
        }

        Map<EnumWirePart, DyeColor> oldParts = Map.copyOf(parts);
        parts.clear();
        if (!holder.getPipeWorld().isClientSide() && initialised) {
            removePartsFromSystem(oldParts.keySet());
        }
        for (Map.Entry<EnumWirePart, DyeColor> entry : oldParts.entrySet()) {
            parts.put(entry.getKey().rotate(rotation), entry.getValue());
        }

        Set<EnumWirePart> rotatedPowered = EnumSet.noneOf(EnumWirePart.class);
        rotatedPowered.addAll(poweredClient);
        poweredClient.clear();
        for (EnumWirePart part : rotatedPowered) {
            poweredClient.add(part.rotate(rotation));
        }

        betweens.clear();
        initialised = false;
        if (!holder.getPipeWorld().isClientSide()) {
            getWireSystems().markStructureChanged();
            BlockEntity tile = holder.getPipeTile();
            if (tile != null) {
                tile.setChanged();
            }
        }
    }

    @Override
    public void updateBetweens(boolean recursive) {
        betweens.clear();
        parts.forEach((part, color) -> {
            for (EnumWireBetween between : EnumWireBetween.VALUES) {
                EnumWirePart[] betweenParts = between.parts;
                if (between.to == null) {
                    if ((betweenParts[0] == part && getColorOfPart(betweenParts[1]) == color)
                        || (betweenParts[1] == part && getColorOfPart(betweenParts[0]) == color)) {
                        betweens.put(between, color);
                    }
                } else if (WireSystem.canWireConnect(holder, between.to)) {
                    IPipe pipe = holder.getNeighbourPipe(between.to);
                    if (pipe != Pipe.EMPTY) {
                        IWireManager wireManager = pipe.getHolder().getWireManager();
                        if (betweenParts[0] == part && wireManager.getColorOfPart(betweenParts[1]) == color) {
                            betweens.put(between, color);
                        }
                    }
                }
            }
        });

        if (!recursive) {
            for (Direction side : Direction.values()) {
                BlockEntity tile = holder.getPipeWorld().getBlockEntity(holder.getPipePos().offset(side.getNormal()));
                if (tile instanceof IPipeHolder) {
                    ((IPipeHolder) tile).getWireManager().updateBetweens(true);
                }
            }
        }
    }

    @Override
    public DyeColor getColorOfPart(EnumWirePart part) {
        return parts.get(part);
    }

    @Override
    public boolean hasPartOfColor(DyeColor color) {
        return parts.values().contains(color);
    }

    @Override
    public boolean isPowered(EnumWirePart part) {
        if (holder.getPipeWorld().isClientSide()) {
            return poweredClient.contains(part);
        } else {
            WorldSavedDataWireSystems wireSystems = this.getWireSystems();
            List<WireSystem> wireSystemsWithElement = wireSystems.getWireSystemsWithElementAsReadOnlyList(new WireSystem.WireElement(holder.getPipePos(), part));
            if (!wireSystemsWithElement.isEmpty()) {
                for (WireSystem wireSystem : wireSystemsWithElement) {
                    Boolean powered = wireSystems.wireSystems.get(wireSystem);
                    if (powered != null && powered) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean isAnyPowered(DyeColor color) {
        if (!this.parts.isEmpty()) {
            for (Map.Entry<EnumWirePart, DyeColor> partColor : this.parts.entrySet()) {
                if (partColor.getValue() == color && this.isPowered(partColor.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        int[] wiresArray = new int[parts.size() * 2];
        int[] i = { 0 };
        parts.forEach((part, color) -> {
            wiresArray[i[0]] = part.ordinal();
            wiresArray[i[0] + 1] = color.getId();
            i[0] += 2;
        });
        nbt.putIntArray("parts", wiresArray);
        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        parts.clear();
        parts.putAll(decodeParts(nbt.getIntArray("parts")));
    }

    static Map<EnumWirePart, DyeColor> decodeParts(int[] wiresArray) {
        Map<EnumWirePart, DyeColor> decoded = new EnumMap<>(EnumWirePart.class);
        for (int i = 0; i + 1 < wiresArray.length; i += 2) {
            int partOrdinal = wiresArray[i];
            int colourId = wiresArray[i + 1];
            if (partOrdinal < 0 || partOrdinal >= EnumWirePart.VALUES.length
                || colourId < 0 || colourId >= DyeColor.values().length) {
                continue;
            }
            decoded.put(EnumWirePart.VALUES[partOrdinal], DyeColor.byId(colourId));
        }
        return decoded;
    }

    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        if (side == LogicalSide.SERVER) {
            buffer.writeInt(parts.size());
            for (Entry<EnumWirePart, DyeColor> entry : parts.entrySet()) {
                buffer.writeEnum(entry.getKey());
                buffer.writeEnum(entry.getValue());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        if (side == LogicalSide.CLIENT) {
            parts.clear();
            int count = buffer.readInt();
            if (count < 0 || count > MAX_PARTS) {
                throw new InvalidInputDataException("Invalid wire part count " + count);
            }
            for (int i = 0; i < count; i++) {
                int partOrdinal = buffer.readVarInt();
                int colourOrdinal = buffer.readVarInt();
                if (partOrdinal < 0 || partOrdinal >= EnumWirePart.VALUES.length
                    || colourOrdinal < 0 || colourOrdinal >= DyeColor.values().length) {
                    throw new InvalidInputDataException(
                        "Invalid wire part entry " + partOrdinal + "/" + colourOrdinal
                    );
                }
                EnumWirePart part = EnumWirePart.VALUES[partOrdinal];
                if (parts.put(part, DyeColor.values()[colourOrdinal]) != null) {
                    throw new InvalidInputDataException("Duplicate wire part " + part);
                }
            }
            updateBetweens(false);
        }
    }
}
