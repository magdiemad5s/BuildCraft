/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;


import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.statements.IAction;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.ITrigger;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.gui.ContainerPipe;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.statement.ActionWrapper;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.lib.statement.TriggerWrapper;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.silicon.BCSiliconGuis;
import buildcraft.silicon.gate.GateContext;
import buildcraft.silicon.gate.GateContext.GateGroup;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateMenuStatePolicy;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class ContainerGate extends ContainerPipe {
    protected static final IdAllocator IDS = MenuBC_Neptune.IDS.makeChild("gate");

    public static final int ID_CONNECTION = IDS.allocId("CONNECTION");
    public static final int ID_VALID_STATEMENTS = IDS.allocId("VALID_STATEMENTS");

    @Nullable
    public final GateLogic gate;

    public final int slotHeight;

    public final SortedSet<TriggerWrapper> possibleTriggers;
    public final SortedSet<ActionWrapper> possibleActions;

    public final GateContext<TriggerWrapper> possibleTriggersContext;
    public final GateContext<ActionWrapper> possibleActionsContext;

    public static ContainerGate creatClientMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos gatePos = buf.readBlockPos();
        Direction gateSide = buf.readEnum(Direction.class);
        ContainerLevelAccess access = ContainerLevelAccess.create(playerInventory.player.level(), gatePos);
        Optional<ContainerGate> resolved = access.evaluate((level, pos) -> {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof IPipeHolder holder) {
                PluggableGate gate = readClientGateSnapshot(holder, gateSide, buf);
                if (gate == null && holder.getPluggable(gateSide) instanceof PluggableGate installedGate) {
                    gate = installedGate;
                }
                if (gate != null) {
                    return Optional.of(new ContainerGate(containerId, playerInventory, gate.logic));
                }
            }
            return Optional.empty();
        }, Optional.empty());
        if (resolved.isPresent()) {
            return resolved.get();
        }
        BCLog.logger.warn("ContainerGate.createClientMenu: gate data at {} on {} is not available yet; closing the fallback menu", gatePos, gateSide);
        return new ContainerGate(containerId, playerInventory);
    }

    private static PluggableGate readClientGateSnapshot(
        IPipeHolder holder,
        Direction gateSide,
        FriendlyByteBuf buffer
    ) {
        if (!buffer.isReadable()) {
            return null;
        }
        try {
            ResourceLocation definitionId = buffer.readResourceLocation();
            PluggableDefinition definition = PipeApi.pluggableRegistry.getDefinition(definitionId);
            if (definition == null) {
                throw new InvalidInputDataException("Unknown gate pluggable definition " + definitionId);
            }
            PipePluggable decoded = definition.loadFromBuffer(holder, gateSide, buffer);
            if (!(decoded instanceof PluggableGate gate)) {
                throw new InvalidInputDataException("Menu snapshot was not a gate: " + definitionId);
            }
            if (holder instanceof TilePipeHolder pipeHolder) {
                pipeHolder.replacePluggable(gateSide, gate);
                return gate;
            }
            // Third-party pipe holders may not expose a safe client-side replacement hook. Keep their installed gate
            // as the live menu model after consuming and validating the authoritative snapshot.
            if (holder.getPluggable(gateSide) instanceof PluggableGate installedGate) {
                return installedGate;
            }
            return gate;
        } catch (IOException | RuntimeException exception) {
            BCLog.logger.error("ContainerGate.createClientMenu: invalid gate snapshot", exception);
            return null;
        }
    }
    
    public ContainerGate(int containerId, Inventory playerInventory, GateLogic logic) {
        super(playerInventory, BCSiliconGuis.MENU_GATE.get(), containerId, logic.getPipeHolder());
        this.gate = logic;
        gate.getPipeHolder().onPlayerOpen(playerInventory.player);


        boolean split = gate.isSplitInTwo();
        int s = gate.variant.numSlots;
        if (split) {
            s = (int) Math.ceil(s / 2.0);
        }
        slotHeight = s;

        if (gate.getPipeHolder().getPipeWorld().isClientSide()) {
            possibleTriggers = new TreeSet<>();
            possibleActions = new TreeSet<>();
        } else {
            possibleTriggers = gate.getAllValidTriggers();
            possibleActions = gate.getAllValidActions();
        }

        possibleTriggersContext = new GateContext<>(new ArrayList<>());
        possibleActionsContext = new GateContext<>(new ArrayList<>());

        refreshPossibleGroups();

        addFullPlayerInventory(33 + slotHeight * 18);
    }

    private ContainerGate(int containerId, Inventory playerInventory) {
        super(playerInventory, BCSiliconGuis.MENU_GATE.get(), containerId, null);
        gate = null;
        slotHeight = 0;
        possibleTriggers = new TreeSet<>();
        possibleActions = new TreeSet<>();
        possibleTriggersContext = new GateContext<>(new ArrayList<>());
        possibleActionsContext = new GateContext<>(new ArrayList<>());
        refreshPossibleGroups();
        addFullPlayerInventory(33);
    }

    public boolean isValidGateMenu() {
        return gate != null && isValidPipeMenu();
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (gate != null) {
            gate.getPipeHolder().onPlayerClose(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (!super.stillValid(player)) {
            return false;
        }
        if (gate == null) {
            return false;
        }
        IPipeHolder holder = gate.getPipeHolder();
        if (holder.getPipeWorld().isClientSide()) {
            return true;
        }
        PluggableGate pluggable = gate.pluggable;
        return holder.getPluggable(pluggable.side) == pluggable;
    }

    private void refreshPossibleGroups() {
        refresh(possibleActions, possibleActionsContext);
        refresh(possibleTriggers, possibleTriggersContext);
    }

    private void refreshServerOptions() {
        if (gate == null) {
            return;
        }
        possibleTriggers.clear();
        possibleTriggers.addAll(gate.getAllValidTriggers());
        possibleActions.clear();
        possibleActions.addAll(gate.getAllValidActions());
        refreshPossibleGroups();
    }

    private static <T extends StatementWrapper> void refresh(SortedSet<T> from, GateContext<T> to) {
        to.groups.clear();
        Map<EnumPipePart, List<T>> parts = new EnumMap<>(EnumPipePart.class);
        for (T val : from) {
            parts.computeIfAbsent(val.getSourcePart(), p -> new ArrayList<>()).add(val);
        }
        List<T> list = parts.get(EnumPipePart.CENTER);
        if (list == null) {
            list = new ArrayList<>(1);
            list.add(null);
        } else {
            list.add(0, null);
        }
        to.groups.add(new GateGroup<>(EnumPipePart.CENTER, list));
        for (EnumPipePart part : EnumPipePart.FACES) {
            list = parts.get(part);
            if (list != null) {
                to.groups.add(new GateGroup<>(part, list));
            }
        }
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        if (gate == null) {
            return;
        }
        if (side == LogicalSide.SERVER) {
            if (id == ID_CONNECTION) {
                int index = buffer.readUnsignedByte();
                boolean to = buffer.readBoolean();
                if (!GateMenuStatePolicy.isSelectableConnectionIndex(
                    gate.variant.numSlots,
                    gate.isSplitInTwo(),
                    index
                )) {
                    throw new InvalidInputDataException("Invalid gate connection index " + index);
                }
                if (gate.connections[index] != to) {
                    gate.connections[index] = to;
                    BlockEntity tile = gate.getPipeHolder().getPipeTile();
                    if (tile != null && tile.getLevel() != null && !tile.getLevel().isClientSide) {
                        tile.setChanged();
                        if (tile instanceof TileBC_Neptune bcTile) {
                            bcTile.markChunkDirty();
                        }
                    }
                    gate.sendResolveData();
                }
            } else if (id == ID_VALID_STATEMENTS) {
                refreshServerOptions();
                sendMessage(ID_VALID_STATEMENTS);
            }
        } else if (side == LogicalSide.CLIENT && id == ID_VALID_STATEMENTS) {
            int numTriggers = buffer.readInt();
            int numActions = buffer.readInt();
            if (!GateMenuStatePolicy.isValidStatementCount(numTriggers)
                || !GateMenuStatePolicy.isValidStatementCount(numActions)) {
                throw new InvalidInputDataException(
                    "Invalid gate statement counts " + numTriggers + "/" + numActions
                );
            }

            SortedSet<TriggerWrapper> newTriggers = new TreeSet<>();
            SortedSet<ActionWrapper> newActions = new TreeSet<>();
            for (int i = 0; i < numTriggers; i++) {
                String tag = buffer.readUtf(GateMenuStatePolicy.MAX_STATEMENT_ID_LENGTH);
                EnumPipePart part = buffer.readEnum(EnumPipePart.class);
                IStatement statement = StatementManager.statements.get(tag);
                if (!(statement instanceof ITrigger)) {
                    throw new InvalidInputDataException("Unknown gate trigger " + tag);
                }
                TriggerWrapper wrapper = TriggerWrapper.wrap(statement, part.face);
                if (!gate.isValidTrigger(wrapper) || !newTriggers.add(wrapper)) {
                    throw new InvalidInputDataException("Invalid or duplicate gate trigger " + tag + "@" + part);
                }
            }
            for (int i = 0; i < numActions; i++) {
                String tag = buffer.readUtf(GateMenuStatePolicy.MAX_STATEMENT_ID_LENGTH);
                EnumPipePart part = buffer.readEnum(EnumPipePart.class);
                IStatement statement = StatementManager.statements.get(tag);
                if (!(statement instanceof IAction)) {
                    throw new InvalidInputDataException("Unknown gate action " + tag);
                }
                ActionWrapper wrapper = ActionWrapper.wrap(statement, part.face);
                if (!gate.isValidAction(wrapper) || !newActions.add(wrapper)) {
                    throw new InvalidInputDataException("Invalid or duplicate gate action " + tag + "@" + part);
                }
            }

            possibleTriggers.clear();
            possibleTriggers.addAll(newTriggers);
            possibleActions.clear();
            possibleActions.addAll(newActions);
            refreshPossibleGroups();
        }
    }

    @Override
    public void writeMessage(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writeMessage(id, buffer, side);
        if (gate == null) {
            return;
        }
        if (side == LogicalSide.SERVER && id == ID_VALID_STATEMENTS) {
            if (!GateMenuStatePolicy.isValidStatementCount(possibleTriggers.size())
                || !GateMenuStatePolicy.isValidStatementCount(possibleActions.size())) {
                throw new IllegalStateException(
                    "Gate has too many valid statements: "
                        + possibleTriggers.size() + "/" + possibleActions.size()
                );
            }
            buffer.writeInt(possibleTriggers.size());
            buffer.writeInt(possibleActions.size());
            for (TriggerWrapper wrapper : possibleTriggers) {
                buffer.writeUtf(wrapper.getUniqueTag(), GateMenuStatePolicy.MAX_STATEMENT_ID_LENGTH);
                buffer.writeEnum(wrapper.getSourcePart());
            }

            for (ActionWrapper wrapper : possibleActions) {
                buffer.writeUtf(wrapper.getUniqueTag(), GateMenuStatePolicy.MAX_STATEMENT_ID_LENGTH);
                buffer.writeEnum(wrapper.getSourcePart());
            }
        }
    }

    public void requestValidStatements() {
        if (gate != null) {
            sendMessage(ID_VALID_STATEMENTS);
        }
    }

    public void setConnected(int index, boolean to) {
        if (gate == null) {
            return;
        }
        sendMessage(ID_CONNECTION, (buffer) -> {
            buffer.writeByte(index);
            buffer.writeBoolean(to);
        });
    }
}
