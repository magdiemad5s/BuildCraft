/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import java.io.IOException;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public final class PluggableHolder {
    // TODO: Networking is kinda sub-par at the moment for pluggables
    // perhaps add some sort of interface for allowing pluggables to correctly write data?
    private static final IdAllocator ID_ALLOC = new IdAllocator("PlugHolder");
    public static final int ID_REMOVE_PLUG = ID_ALLOC.allocId("REMOVE_PLUG");
    public static final int ID_UPDATE_PLUG = ID_ALLOC.allocId("UPDATE_PLUG");
    public static final int ID_CREATE_PLUG = ID_ALLOC.allocId("CREATE_PLUG");

    public final TilePipeHolder holder;
    public /*final*/ Direction side;
    public PipePluggable pluggable = PipePluggable.EMPTY;

    public PluggableHolder(TilePipeHolder holder, Direction side) {
        this.holder = holder;
        this.side = side;
    }

    // Saving + Loading

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        if (pluggable != PipePluggable.EMPTY) {
            nbt.putString("id", pluggable.definition.identifier.toString());
            nbt.put("data", pluggable.writeToNbt());
        }
        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            replaceLocal(PipePluggable.EMPTY);
            return;
        }
        String id = nbt.getString("id");
        CompoundTag data = nbt.getCompound("data");
        try {
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier == null) {
                BCLog.logger.warn("Invalid pluggable id '{}' in pipe NBT; removing the pluggable", id);
                replaceLocal(PipePluggable.EMPTY);
                return;
            }
            PluggableDefinition def = PipeApi.pluggableRegistry.getDefinition(identifier);
            if (def == null) {
                BCLog.logger.warn("Unknown pluggable id '{}' in pipe NBT; removing the pluggable", id);
                replaceLocal(PipePluggable.EMPTY);
                return;
            }
            PipePluggable loaded = def.readFromNbt(holder, side, data);
            replaceLocal(loaded == null ? PipePluggable.EMPTY : loaded);
        } catch (RuntimeException exception) {
            BCLog.logger.warn("Failed to load pluggable '{}' from pipe NBT; removing the pluggable", id, exception);
            replaceLocal(PipePluggable.EMPTY);
        }
    }

    // Network

    /** Called by {@link TilePipeHolder#replacePluggable(Direction, PipePluggable)} to inform clients about the new
     * pluggable. */
    public void sendNewPluggableData() {
        holder.sendMessage(PipeMessageReceiver.PLUGGABLES[side.ordinal()], this::writeCreationPayload);
    }

    public void writeCreationPayload(FriendlyByteBuf buffer) {
        if (pluggable == PipePluggable.EMPTY) {
            buffer.writeByte(ID_REMOVE_PLUG);
        } else {
            buffer.writeByte(ID_CREATE_PLUG);
            buffer.writeUtf(pluggable.definition.identifier.toString(), 64);
            pluggable.writeCreationPayload(buffer);
        }
    }

    public void readCreationPayload(FriendlyByteBuf buffer) throws InvalidInputDataException {
        int id = buffer.readUnsignedByte();
        if (id == ID_CREATE_PLUG) {
            readCreateInternal(buffer);
        } else if (id == ID_REMOVE_PLUG) {
            replaceLocal(PipePluggable.EMPTY);
        } else {
            throw new InvalidInputDataException("Invalid ID for creation! " + ID_ALLOC.getNameFor(id));
        }
    }

    private void readCreateInternal(FriendlyByteBuf buffer) throws InvalidInputDataException {
        String rawIdentifier = buffer.readUtf(64);
        ResourceLocation identifier = ResourceLocation.tryParse(rawIdentifier);
        if (identifier == null) {
            throw new InvalidInputDataException("Invalid remote pluggable id \"" + rawIdentifier + "\"");
        }
        PluggableDefinition def = PipeApi.pluggableRegistry.getDefinition(identifier);
        if (def == null) {
            throw new InvalidInputDataException("Unknown remote pluggable \"" + identifier + "\"");
        }
        PipePluggable loaded;
        try {
            loaded = def.loadFromBuffer(holder, side, buffer);
        } catch (InvalidInputDataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidInputDataException("Invalid data for remote pluggable \"" + identifier + "\"", exception);
        }
        if (loaded == null || loaded == PipePluggable.EMPTY) {
            throw new InvalidInputDataException("Remote pluggable loader returned no pluggable for \"" + identifier + "\"");
        }
        replaceLocal(loaded);
    }

    private void replaceLocal(PipePluggable replacement) {
        if (pluggable != PipePluggable.EMPTY) {
            holder.eventBus.unregisterHandler(pluggable);
        }
        pluggable = replacement;
        if (pluggable != PipePluggable.EMPTY) {
            holder.eventBus.registerHandler(pluggable);
        }
    }

    public void writePayload(FriendlyByteBuf buffer, LogicalSide netSide) {
        if (netSide == LogicalSide.CLIENT) {
            buffer.writeByte(ID_UPDATE_PLUG);
            if (pluggable != PipePluggable.EMPTY) {
                pluggable.writePayload(buffer, netSide);
            }
        } else {
            if (pluggable == PipePluggable.EMPTY) {
                buffer.writeByte(ID_REMOVE_PLUG);
            } else {
                buffer.writeByte(ID_UPDATE_PLUG);
                pluggable.writePayload(buffer, netSide);
            }
        }
    }

    public void readPayload(FriendlyByteBuf buffer, LogicalSide netSide, NetworkEvent.Context ctx) throws IOException {
        int id = buffer.readUnsignedByte();
        if (netSide == LogicalSide.SERVER) {
            if (id == ID_UPDATE_PLUG) {
                if (pluggable != PipePluggable.EMPTY) {
                    pluggable.readPayload(buffer, netSide, ctx);
                }
            } else {
                throw new InvalidInputDataException("Unknown ID " + ID_ALLOC.getNameFor(id));
            }
        } else {
            if (id == ID_REMOVE_PLUG) {
                replaceLocal(PipePluggable.EMPTY);
            } else if (id == ID_UPDATE_PLUG) {
                pluggable.readPayload(buffer, netSide, ctx);
            } else if (id == ID_CREATE_PLUG) {
                readCreateInternal(buffer);
            } else {
                throw new InvalidInputDataException("Unknown ID " + ID_ALLOC.getNameFor(id));
            }
        }
    }

    // Pluggable overrides

    public void onTick() {
        if (pluggable != PipePluggable.EMPTY) {
            pluggable.onTick();
        }
    }

	public void rotate(Rotation axis) {
		side = axis.rotate(side);
		pluggable.rotate(axis);
	}
}
