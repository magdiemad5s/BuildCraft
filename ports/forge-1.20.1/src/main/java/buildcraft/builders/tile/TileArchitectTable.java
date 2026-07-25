/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.builders.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.IAreaProvider;
import buildcraft.api.enums.EnumSnapshotType;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.ISchematicEntity;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.schematics.SchematicEntityContext;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.client.ClientArchitectTables;
import buildcraft.builders.gui.MenuArchitectTable;
import buildcraft.builders.snapshot.CompositeBlueprintCapture;
import buildcraft.builders.snapshot.CompositeBlueprintCapture.PlanException;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.menu.ArchitectNamePolicy;
import buildcraft.builders.menu.ContainerArchitectTable;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.GlobalSavedDataSnapshots;
import buildcraft.builders.snapshot.SchematicBlockManager;
import buildcraft.builders.snapshot.SchematicEntityManager;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Snapshot.Header;
import buildcraft.builders.snapshot.Template;
import buildcraft.core.marker.volume.Lock;
import buildcraft.core.marker.volume.VolumeBox;
import buildcraft.core.marker.volume.WorldSavedDataVolumeBoxes;
import buildcraft.lib.delta.DeltaInt;
import buildcraft.lib.delta.DeltaManager;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.BoundingBoxUtil;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.data.Box;
import buildcraft.lib.misc.data.BoxIterator;
import buildcraft.lib.misc.data.EnumAxisOrder;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class TileArchitectTable extends TileBC_Neptune implements IDebuggable, MenuProvider{

	public static final IdAllocator IDS = TileBC_Neptune.IDS.makeChild("architect");
    public static final int MAX_SUB_BLUEPRINTS = 256;

    public static final int NET_BOX = IDS.allocId("BOX");
    public static final int NET_SCAN = IDS.allocId("SCAN");
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftbuilders:architect");

    public final ItemHandlerSimple invSnapshotIn = itemManager.addInvHandler(
        "in",
        1,
        (slot, stack) -> stack.getItem() instanceof ItemSnapshot,
        EnumAccess.INSERT,
        EnumPipePart.VALUES
    );
    public final ItemHandlerSimple invSnapshotOut = itemManager.addInvHandler(
        "out",
        1,
        EnumAccess.EXTRACT,
        EnumPipePart.VALUES
    );

    private EnumSnapshotType snapshotType = EnumSnapshotType.BLUEPRINT;
    public final Box box = new Box();
    public boolean markerBox = false;
    public final List<BlockPos> subBlueprints = new ArrayList<>();
    private BitSet templateScannedBlocks;
    private final List<ISchematicBlock> blueprintScannedPalette = new ArrayList<>();
    private int[] blueprintScannedData;
    private final List<ISchematicEntity> blueprintScannedEntities = new ArrayList<>();
    private BoxIterator boxIterator;
    private boolean isValid = false;
    private boolean scanning = false;
    public String name = "<unnamed>";
    private CompositeBlueprintCapture compositeCapture;
    private boolean scanFailed;
    private String compositionError = "";

    public final DeltaInt deltaProgress = deltaManager.addDelta(
        "progress",
        DeltaManager.EnumNetworkVisibility.GUI_ONLY
    );
    
    private boolean allowCreative = false;
    private boolean canRotate = true;
    private boolean canExcavate = true;
    
    private DataSlot menuSetting = new DataSlot() {
		
		@Override
		public void set(int p) {
            setSnapshotSettings(p, true);
		}
		
		@Override
		public int get() {
            return getSnapshotSettings();
		}
	};

    private static DataSlot createCreativePermissionSlot(Player player) {
        return new DataSlot() {
            @Override
            public int get() {
                return canPlayerUseCreativeBlueprintMode(player) ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        };
    }
    
    public TileArchitectTable(BlockPos pos, BlockState state) {
		super(BCBuildersBlocks.ARCHITECT_TILE_BC8.get(), pos, state);
	}

    public static boolean canPlayerUseCreativeBlueprintMode(Player player) {
        return player != null && (player.isCreative() || player.hasPermissions(2));
    }

    public int getSnapshotSettings() {
        return (allowCreative ? 1 : 0) | (canRotate ? 0b10 : 0) | (canExcavate ? 0b100 : 0);
    }

    public void setSnapshotSettingsFromPlayer(int settings, Player player) {
        setSnapshotSettings(settings, canPlayerUseCreativeBlueprintMode(player));
    }

    public void setSnapshotName(String requestedName) {
        String sanitized = ArchitectNamePolicy.sanitize(requestedName);
        if (Objects.equals(name, sanitized)) {
            return;
        }
        name = sanitized;
        setChanged();
        if (level != null && !level.isClientSide) {
            sendNetworkUpdate(NET_RENDER_DATA);
        }
    }

    private void setSnapshotSettings(int settings, boolean canUseCreativeMode) {
        boolean oldAllowCreative = allowCreative;
        boolean oldCanRotate = canRotate;
        boolean oldCanExcavate = canExcavate;

        allowCreative = canUseCreativeMode && (settings & 0b1) == 0b1;
        canRotate = (settings & 0b10) == 0b10;
        canExcavate = (settings & 0b100) == 0b100;

        if (oldAllowCreative != allowCreative || oldCanRotate != canRotate || oldCanExcavate != canExcavate) {
            setChanged();
        }
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    /** Modern equivalent of the released Architect {@code COPY} mode. */
    public boolean isBlueprintCopyMode() {
        if (!isValid || scanning) {
            return false;
        }
        ItemStack input = invSnapshotIn.getStackInSlot(0);
        if (input.isEmpty()) {
            return snapshotType == EnumSnapshotType.BLUEPRINT;
        }
        return input.getItem() instanceof ItemSnapshot
            && ItemSnapshot.EnumItemSnapshotType.getFromStack(input).snapshotType == EnumSnapshotType.BLUEPRINT;
    }

    /** Adds one persisted composite-blueprint endpoint, preserving the released insertion order. */
    public boolean addSubBlueprint(BlockPos pos) {
        if (level == null || level.isClientSide || !isBlueprintCopyMode() || pos == null
            || pos.equals(worldPosition) || subBlueprints.contains(pos)
            || subBlueprints.size() >= MAX_SUB_BLUEPRINTS) {
            return false;
        }
        subBlueprints.add(pos.immutable());
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
        return true;
    }

    public List<BlockPos> getSubBlueprints() {
        return List.copyOf(subBlueprints);
    }

    @Override
    public void onPlacedBy(LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (placer.level().isClientSide) {
            return;
        }
        WorldSavedDataVolumeBoxes volumeBoxes = WorldSavedDataVolumeBoxes.get(level);
        BlockState blockState = level.getBlockState(worldPosition);
        BlockPos offsetPos = worldPosition.offset(blockState.getValue(BlockArchitectTable.PROP_FACING).getOpposite().getNormal());
        VolumeBox volumeBox = volumeBoxes.getVolumeBoxAt(offsetPos);
        BlockEntity tile = level.getBlockEntity(offsetPos);
        if (volumeBox != null) {
            box.reset();
            box.setMin(volumeBox.box.min());
            box.setMax(volumeBox.box.max());
            isValid = true;
            volumeBox.locks.add(
                new Lock(
                    new Lock.Cause.CauseBlock(worldPosition, blockState.getBlock()),
                    new Lock.Target.TargetRemove(),
                    new Lock.Target.TargetResize(),
                    new Lock.Target.TargetUsedByMachine(
                        Lock.Target.TargetUsedByMachine.EnumType.STRIPES_READ
                    )
                )
            );
            volumeBoxes.setDirty();
            sendNetworkUpdate(NET_BOX);
        } else if (tile instanceof IAreaProvider) {
            IAreaProvider provider = (IAreaProvider) tile;
            box.reset();
            box.setMin(provider.min());
            box.setMax(provider.max());
            markerBox = true;
            isValid = true;
            provider.removeFromWorld(placer instanceof Player player? player : null);
        } else {
            isValid = false;
            BlockState state = level.getBlockState(worldPosition);
            state = state.setValue(BlockArchitectTable.PROP_VALID, Boolean.FALSE);
            level.setBlockAndUpdate(worldPosition, state);
        }
    }

    @Override
    public void update() {
        deltaManager.tick();

        if (level.isClientSide) {
            if (box.isInitialized()) {
                ClientArchitectTables.BOXES.put(box.getBoundingBox(), ClientArchitectTables.START_BOX_VALUE);
            }
            return;
        }

        if (!invSnapshotIn.getStackInSlot(0).isEmpty() && invSnapshotOut.getStackInSlot(0).isEmpty() && isValid) {
            if (!scanning && !beginScan()) {
                return;
            }
        } else {
            if (scanning) {
                resetScanBuffers();
            }
            scanning = false;
        }

        if (scanning) {
            scanMultipleBlocks();
            if (!scanning) {
                if (scanFailed) {
                    resetScanBuffers();
                    scanFailed = false;
                    return;
                }
                if (snapshotType == EnumSnapshotType.BLUEPRINT) {
                    scanEntities();
                }
                finishScanning();
            }
        }
    }

    private boolean beginScan() {
        snapshotType = ItemSnapshot.EnumItemSnapshotType.getFromStack(
            invSnapshotIn.getStackInSlot(0)
        ).snapshotType;
        compositeCapture = null;
        scanFailed = false;

        if (snapshotType == EnumSnapshotType.BLUEPRINT && !subBlueprints.isEmpty()) {
            try {
                compositeCapture = CompositeBlueprintCapture.create(this);
                compositionError = "";
            } catch (PlanException exception) {
                String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                if (!error.equals(compositionError)) {
                    BCLog.logger.warn("Unable to start composite blueprint at " + worldPosition + ": " + error);
                }
                compositionError = error;
                return false;
            }
        } else {
            compositionError = "";
        }

        int blockCount = compositeCapture == null
            ? Snapshot.getDataSize(box.size())
            : compositeCapture.getWorkSize();
        int scanTicks = Math.max(1, (blockCount + snapshotType.maxPerTick - 1) / snapshotType.maxPerTick);
        deltaProgress.addDelta(0, scanTicks, 1);
        deltaProgress.addDelta(scanTicks, scanTicks + 10, -1);
        scanning = true;
        return true;
    }

    private void resetScanBuffers() {
        templateScannedBlocks = null;
        blueprintScannedPalette.clear();
        blueprintScannedData = null;
        blueprintScannedEntities.clear();
        boxIterator = null;
        compositeCapture = null;
    }

    private void scanMultipleBlocks() {
        for (int i = snapshotType.maxPerTick; i > 0; i--) {
            scanSingleBlock();
            if (!scanning) {
                break;
            }
        }
    }

    private void scanSingleBlock() {
        if (compositeCapture != null) {
            try {
                compositeCapture.captureNext();
                BlockPos levelScanPos = compositeCapture.getLastCapturedPos();
                if (levelScanPos != null) {
                    createAndSendMessage(NET_SCAN, buffer -> MessageUtil.writeBlockPos(buffer, levelScanPos));
                }
                if (compositeCapture.isFinished()) {
                    scanning = false;
                }
                sendNetworkUpdate(NET_RENDER_DATA);
            } catch (PlanException exception) {
                compositionError = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
                BCLog.logger.warn("Composite blueprint scan failed at " + worldPosition + ": " + compositionError);
                scanFailed = true;
                scanning = false;
            }
            return;
        }

        BlockPos size = box.size();
        if (templateScannedBlocks == null || blueprintScannedData == null) {
            boxIterator = new BoxIterator(box, EnumAxisOrder.XZY.getMinToMaxOrder(), true);
            templateScannedBlocks = new BitSet(Snapshot.getDataSize(size));
            blueprintScannedData = new int[Snapshot.getDataSize(size)];
        }

        // Read from level
        BlockPos levelScanPos = boxIterator.getCurrent();
        BlockPos schematicPos = levelScanPos.subtract(box.min());
        if (snapshotType == EnumSnapshotType.TEMPLATE) {
            templateScannedBlocks.set(Snapshot.posToIndex(box.size(), schematicPos), !level.isEmptyBlock(levelScanPos));
        }
        if (snapshotType == EnumSnapshotType.BLUEPRINT) {
            ISchematicBlock schematicBlock = readSchematicBlock(levelScanPos);
            int index = blueprintScannedPalette.indexOf(schematicBlock);
            if (index == -1) {
                index = blueprintScannedPalette.size();
                blueprintScannedPalette.add(schematicBlock);
            }
            blueprintScannedData[Snapshot.posToIndex(box.size(), schematicPos)] = index;
        }

        createAndSendMessage(NET_SCAN, buffer -> MessageUtil.writeBlockPos(buffer, levelScanPos));

        sendNetworkUpdate(NET_RENDER_DATA);

        // Move scanPos along
        boxIterator.advance();

        if (boxIterator.hasFinished()) {
            scanning = false;
            boxIterator = null;
        }
    }

    private ISchematicBlock readSchematicBlock(BlockPos levelScanPos) {
        return SchematicBlockManager.getSchematicBlock(new SchematicBlockContext(
            level,
            box.min(),
            levelScanPos,
            level.getBlockState(levelScanPos),
            level.getBlockState(levelScanPos).getBlock()
        ));
    }

    private void scanEntities() {
        if (compositeCapture != null) {
            compositeCapture.collectEntities(blueprintScannedEntities);
            return;
        }
        level.getEntitiesOfClass(Entity.class, box.getBoundingBox()).stream()
            .map(entity ->
                SchematicEntityManager.getSchematicEntity(new SchematicEntityContext(
                    level,
                    box.min(),
                    entity
                ))
            )
            .filter(Objects::nonNull)
            .forEach(blueprintScannedEntities::add);
    }

    private void finishScanning() {
        BlockState thisState = getCurrentStateForBlock(BCBuildersBlocks.ARCHITECT.get());
        if (thisState == null) {
            resetScanBuffers();
            return;
        }

        var knownOwner = getKnownOwner();
        var ownerProfile = getOwner();

        Direction facing = thisState.getValue(BlockArchitectTable.PROP_FACING);
        Snapshot snapshot = Snapshot.create(snapshotType);
        BlockPos snapshotMin = compositeCapture == null ? box.min() : compositeCapture.getMin();
        snapshot.size = compositeCapture == null ? box.size() : compositeCapture.getSize();
        snapshot.facing = facing;
        snapshot.offset = snapshotMin.subtract(worldPosition.offset(facing.getOpposite().getNormal()));
        if (snapshot instanceof Template) {
            ((Template) snapshot).data = templateScannedBlocks;
        }
        if (snapshot instanceof Blueprint) {
            ((Blueprint) snapshot).palette.addAll(
                compositeCapture == null ? blueprintScannedPalette : compositeCapture.getPalette()
            );
            ((Blueprint) snapshot).data = compositeCapture == null
                ? blueprintScannedData
                : compositeCapture.getData();
            ((Blueprint) snapshot).entities.addAll(blueprintScannedEntities);
        }
        snapshot.computeKey();
        GlobalSavedDataSnapshots.get(level).addSnapshot(snapshot);
        ItemStack stackIn = invSnapshotIn.getStackInSlot(0);
        stackIn.setCount(stackIn.getCount() - 1);
        if (stackIn.getCount() == 0) {
            stackIn = ItemStack.EMPTY;
        }
        invSnapshotIn.setStackInSlot(0, stackIn);
        invSnapshotOut.setStackInSlot(
            0,
            ItemSnapshot.getUsed(
                snapshotType,
                new Header(
                    snapshot.key,
                    ownerProfile.getId(),
                    ownerProfile.getName(),
                    new Date(),
                    name,
                    allowCreative,
                    canRotate,
                    canExcavate
                )
            )
        );
        resetScanBuffers();
        sendNetworkUpdate(NET_RENDER_DATA);
        if (knownOwner != null && knownOwner.getId() != null) {
            AdvancementUtil.unlockAdvancement(knownOwner.getId(), ADVANCEMENT);
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                writePayload(NET_BOX, buffer, side);
                buffer.writeUtf(name, ArchitectNamePolicy.MAX_LENGTH);
                buffer.writeVarInt(subBlueprints.size());
                for (BlockPos subBlueprint : subBlueprints) {
                    buffer.writeBlockPos(subBlueprint);
                }
            } else if (id == NET_BOX) {
                box.writeData(buffer);
                buffer.writeBoolean(markerBox);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
    	super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                readPayload(NET_BOX, buffer, side, ctx);
                name = ArchitectNamePolicy.sanitize(buffer.readUtf(ArchitectNamePolicy.MAX_LENGTH));
                subBlueprints.clear();
                int count = buffer.readVarInt();
                for (int i = 0; i < count; i++) {
                    BlockPos pos = buffer.readBlockPos();
                    if (subBlueprints.size() < MAX_SUB_BLUEPRINTS) {
                        subBlueprints.add(pos);
                    }
                }
            } else if (id == NET_BOX) {
                box.readData(buffer);
                markerBox = buffer.readBoolean();
            } else if (id == NET_SCAN) {
                ClientArchitectTables.SCANNED_BLOCKS.put(
                    MessageUtil.readBlockPos(buffer),
                    ClientArchitectTables.START_SCANNED_BLOCK_VALUE
                );
            }
        }
    }

    @Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
        nbt.put("box", box.writeToNBT());
        nbt.putBoolean("markerBox", markerBox);
        if (boxIterator != null) {
            nbt.put("iter", boxIterator.writeToNbt());
        }
        nbt.putBoolean("scanning", scanning);
        nbt.put("snapshotType", NBTUtilBC.writeEnum(snapshotType));
        nbt.putBoolean("isValid", isValid);
        nbt.putString("name", name);
        nbt.putBoolean("allowCreative", allowCreative);
        nbt.putBoolean("canRotate", canRotate);
        nbt.putBoolean("canExcavate", canExcavate);
        ListTag subBlueprintList = new ListTag();
        for (BlockPos subBlueprint : subBlueprints) {
            subBlueprintList.add(NbtUtils.writeBlockPos(subBlueprint));
        }
        nbt.put("subBlueprints", subBlueprintList);
	}

    @Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
        box.initialize(nbt.getCompound("box"));
        markerBox = nbt.getBoolean("markerBox");
        // Scan buffers are transient; restart a partial normal/composite scan from the beginning.
        boxIterator = null;
        scanning = false;
        snapshotType = NBTUtilBC.readEnum(nbt.get("snapshotType"), EnumSnapshotType.class);
        if (snapshotType == null) {
            snapshotType = EnumSnapshotType.BLUEPRINT;
        }
        isValid = nbt.getBoolean("isValid");
        name = ArchitectNamePolicy.sanitize(nbt.getString("name"));
        allowCreative = nbt.contains("allowCreative") && nbt.getBoolean("allowCreative");
        canRotate = !nbt.contains("canRotate") || nbt.getBoolean("canRotate");
        canExcavate = !nbt.contains("canExcavate") || nbt.getBoolean("canExcavate");
        subBlueprints.clear();
        ListTag subBlueprintList = nbt.getList("subBlueprints", Tag.TAG_COMPOUND);
        for (int i = 0; i < subBlueprintList.size() && subBlueprints.size() < MAX_SUB_BLUEPRINTS; i++) {
            BlockPos pos = NbtUtils.readBlockPos(subBlueprintList.getCompound(i));
            if (!pos.equals(worldPosition) && !subBlueprints.contains(pos)) {
                subBlueprints.add(pos);
            }
        }
	}

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("box:");
        left.add(" - min = " + box.min());
        left.add(" - max = " + box.max());
        left.add("scanning = " + scanning);
        left.add("current = " + (boxIterator == null ? null : boxIterator.getCurrent()));
        left.add("subBlueprints = " + subBlueprints.size());
        left.add("compositionError = " + compositionError);
        left.add("compositionCycles = " + (compositeCapture == null ? 0 : compositeCapture.getCycleCount()));
    }

    // Rendering

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
	public AABB getRenderBoundingBox() {
		return BoundingBoxUtil.makeFrom(worldPosition, box, subBlueprints);
	}
/*
	@Override
    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return Double.MAX_VALUE;
    }*/

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (!canPlayerUseCreativeBlueprintMode(player) && allowCreative) {
            allowCreative = false;
            setChanged();
        }
		return new ContainerArchitectTable(id, inventory, invSnapshotIn, invSnapshotOut, menuSetting,
            createCreativePermissionSlot(player), /* deltaProgress.getContainerData(), */ContainerLevelAccess.create(getLevel(), worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}
}
