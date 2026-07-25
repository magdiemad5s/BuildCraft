/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.ISchematicEntity;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.schematics.SchematicEntityContext;
import buildcraft.builders.item.ConstructionMarkerLinkState;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileConstructionMarker;
import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Incremental, bounded capture plan for released-style composite blueprints.
 *
 * <p>Architect regions are read from the world over the normal per-tick scan budget. Existing blueprints in linked
 * Builders and Construction Markers are copied through their current placement transform. Components are flattened in
 * released order (parent first, then children), with an explicit ignore schematic in sparse gaps.</p>
 */
public final class CompositeBlueprintCapture {
    public static final int MAX_DEPTH = 32;
    public static final int MAX_COMPONENTS = TileArchitectTable.MAX_SUB_BLUEPRINTS;

    private final Level level;
    private final List<Component> components;
    private final BlockPos min;
    private final BlockPos max;
    private final BlockPos size;
    private final int workSize;
    private final List<ISchematicBlock> palette = new ArrayList<>();
    private final int[] data;
    private final int cycleCount;

    private int componentIndex;
    private int componentCellIndex;
    private BlockPos lastCapturedPos;

    private CompositeBlueprintCapture(Level level, List<Component> components, int cycleCount) throws PlanException {
        this.level = level;
        this.components = List.copyOf(components);
        this.cycleCount = cycleCount;

        BlockPos computedMin = null;
        BlockPos computedMax = null;
        long computedWorkSize = 0;
        for (Component component : components) {
            computedMin = min(computedMin, component.min());
            computedMax = max(computedMax, component.max());
            computedWorkSize += component.cellCount();
        }
        if (computedMin == null || computedMax == null) {
            throw new PlanException("Composite blueprint has no capture regions");
        }
        if (computedWorkSize <= 0 || computedWorkSize > Snapshot.MAX_SNAPSHOT_BLOCKS) {
            throw new PlanException("Composite blueprint scan contains " + computedWorkSize + " component blocks");
        }

        min = computedMin;
        max = computedMax;
        size = max.subtract(min).offset(1, 1, 1);
        try {
            data = new int[Snapshot.getDataSize(size)];
        } catch (IllegalArgumentException exception) {
            throw new PlanException(exception.getMessage(), exception);
        }
        workSize = (int) computedWorkSize;

        // Index zero is intentionally the safe sparse-cell default for the freshly zero-filled data array.
        palette.add(new SchematicBlockIgnore());
        Arrays.fill(data, 0);
    }

    public static CompositeBlueprintCapture create(TileArchitectTable root) throws PlanException {
        Level level = root.getLevel();
        if (level == null || level.isClientSide) {
            throw new PlanException("Composite blueprints can only be captured on a logical server");
        }
        Collector collector = new Collector(level);
        collector.addArchitect(root, 0);
        return new CompositeBlueprintCapture(level, collector.components, collector.cycleCount);
    }

    public boolean captureNext() throws PlanException {
        if (isFinished()) {
            return false;
        }
        Component component = components.get(componentIndex);
        CapturedCell cell = component.capture(level, componentCellIndex);
        lastCapturedPos = cell.worldPos();
        put(cell.worldPos(), cell.schematicBlock());

        componentCellIndex++;
        if (componentCellIndex >= component.cellCount()) {
            componentIndex++;
            componentCellIndex = 0;
        }
        return true;
    }

    private void put(BlockPos worldPos, ISchematicBlock schematicBlock) throws PlanException {
        BlockPos relative = toCompositePosition(worldPos, min);
        if (relative.getX() < 0 || relative.getY() < 0 || relative.getZ() < 0
            || relative.getX() >= size.getX() || relative.getY() >= size.getY() || relative.getZ() >= size.getZ()) {
            throw new PlanException("Composite cell " + worldPos + " lies outside " + min + " -> " + max);
        }
        int paletteIndex = findPaletteIndex(schematicBlock);
        data[Snapshot.posToIndex(size, relative)] = paletteIndex;
    }

    private int findPaletteIndex(ISchematicBlock schematicBlock) {
        for (int i = 0; i < palette.size(); i++) {
            if (Blueprint.schematicMatchesForReplacement(palette.get(i), schematicBlock)) {
                return i;
            }
        }
        palette.add(schematicBlock);
        return palette.size() - 1;
    }

    public void collectEntities(List<ISchematicEntity> output) {
        Set<UUID> capturedWorldEntities = new HashSet<>();
        for (Component component : components) {
            if (component instanceof WorldComponent worldComponent) {
                for (Entity entity : level.getEntitiesOfClass(Entity.class, worldComponent.box.getBoundingBox())) {
                    if (!capturedWorldEntities.add(entity.getUUID())) {
                        continue;
                    }
                    ISchematicEntity schematic = SchematicEntityManager.getSchematicEntity(
                        new SchematicEntityContext(level, min, entity)
                    );
                    if (schematic != null) {
                        output.add(schematic);
                    }
                }
            } else if (component instanceof SnapshotComponent snapshotComponent) {
                BlockPos translation = snapshotComponent.buildingInfo.offsetPos.subtract(min);
                for (ISchematicEntity entity : snapshotComponent.buildingInfo.entities) {
                    if (entity instanceof SchematicEntityDefault defaultEntity) {
                        output.add(defaultEntity.getTranslated(translation));
                    }
                }
            }
        }
    }

    public boolean isFinished() {
        return componentIndex >= components.size();
    }

    public int getWorkSize() {
        return workSize;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public BlockPos getSize() {
        return size;
    }

    public BlockPos getLastCapturedPos() {
        return lastCapturedPos;
    }

    public List<ISchematicBlock> getPalette() {
        return List.copyOf(palette);
    }

    public int[] getData() {
        return data;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    static BlockPos toCompositePosition(BlockPos worldPos, BlockPos compositeMin) {
        return worldPos.subtract(compositeMin);
    }

    private static BlockPos min(BlockPos first, BlockPos second) {
        if (first == null) {
            return second;
        }
        return new BlockPos(
            Math.min(first.getX(), second.getX()),
            Math.min(first.getY(), second.getY()),
            Math.min(first.getZ(), second.getZ())
        );
    }

    private static BlockPos max(BlockPos first, BlockPos second) {
        if (first == null) {
            return second;
        }
        return new BlockPos(
            Math.max(first.getX(), second.getX()),
            Math.max(first.getY(), second.getY()),
            Math.max(first.getZ(), second.getZ())
        );
    }

    private interface Component {
        BlockPos min();

        BlockPos max();

        int cellCount();

        CapturedCell capture(Level level, int index) throws PlanException;
    }

    private static final class WorldComponent implements Component {
        private final Box box;
        private final BlockPos size;
        private final int cellCount;

        private WorldComponent(Box source) throws PlanException {
            if (source == null || !source.isInitialized()) {
                throw new PlanException("Linked Architect has no capture box");
            }
            box = new Box(source.min(), source.max());
            size = box.size();
            try {
                cellCount = Snapshot.getDataSize(size);
            } catch (IllegalArgumentException exception) {
                throw new PlanException(exception.getMessage(), exception);
            }
        }

        @Override
        public BlockPos min() {
            return box.min();
        }

        @Override
        public BlockPos max() {
            return box.max();
        }

        @Override
        public int cellCount() {
            return cellCount;
        }

        @Override
        public CapturedCell capture(Level level, int index) {
            BlockPos worldPos = Snapshot.indexToPos(size, index).offset(box.min());
            BlockState state = level.getBlockState(worldPos);
            ISchematicBlock schematic = SchematicBlockManager.getSchematicBlock(
                new SchematicBlockContext(level, box.min(), worldPos, state, state.getBlock())
            );
            return new CapturedCell(worldPos, schematic);
        }
    }

    private static final class SnapshotComponent implements Component {
        private final Blueprint.BuildingInfo buildingInfo;
        private final Blueprint snapshot;

        private SnapshotComponent(Blueprint.BuildingInfo buildingInfo) throws PlanException {
            if (buildingInfo == null) {
                throw new PlanException("Linked Builder or Construction Marker has no loaded blueprint");
            }
            this.buildingInfo = buildingInfo;
            snapshot = buildingInfo.getSnapshot();
            if (snapshot.data == null || snapshot.data.length != snapshot.getDataSize()) {
                throw new PlanException("Linked blueprint has invalid block data");
            }
        }

        @Override
        public BlockPos min() {
            return buildingInfo.box.min();
        }

        @Override
        public BlockPos max() {
            return buildingInfo.box.max();
        }

        @Override
        public int cellCount() {
            return snapshot.getDataSize();
        }

        @Override
        public CapturedCell capture(Level level, int index) throws PlanException {
            int paletteIndex = snapshot.data[index];
            if (paletteIndex < 0 || paletteIndex >= buildingInfo.rotatedPalette.size()) {
                throw new PlanException("Linked blueprint palette index " + paletteIndex + " is invalid");
            }
            BlockPos localPos = snapshot.indexToPos(index);
            return new CapturedCell(
                buildingInfo.toWorld(localPos),
                buildingInfo.rotatedPalette.get(paletteIndex)
            );
        }
    }

    private record CapturedCell(BlockPos worldPos, ISchematicBlock schematicBlock) {
    }

    private static final class Collector {
        private final Level level;
        private final List<Component> components = new ArrayList<>();
        private final Set<BlockPos> visitedArchitects = new LinkedHashSet<>();
        private int cycleCount;

        private Collector(Level level) {
            this.level = level;
        }

        private void addArchitect(TileArchitectTable architect, int depth) throws PlanException {
            if (depth > MAX_DEPTH) {
                throw new PlanException("Composite blueprint nesting exceeds " + MAX_DEPTH + " levels");
            }
            BlockPos architectPos = architect.getBlockPos();
            if (!visitedArchitects.add(architectPos)) {
                cycleCount++;
                return;
            }
            addComponent(new WorldComponent(architect.box));

            for (BlockPos linkedPos : architect.getSubBlueprints()) {
                if (!ConstructionMarkerLinkState.withinReach(
                    architectPos.getX(), architectPos.getY(), architectPos.getZ(),
                    linkedPos.getX(), linkedPos.getY(), linkedPos.getZ()
                )) {
                    throw new PlanException("Composite link " + architectPos + " -> " + linkedPos + " exceeds 64 blocks");
                }
                if (!level.hasChunkAt(linkedPos)) {
                    throw new PlanException("Composite endpoint " + linkedPos + " is not loaded");
                }
                BlockEntity linked = level.getBlockEntity(linkedPos);
                if (linked instanceof TileArchitectTable childArchitect) {
                    if (!childArchitect.isBlueprintCopyMode()) {
                        throw new PlanException("Linked Architect at " + linkedPos + " is not in blueprint copy mode");
                    }
                    addArchitect(childArchitect, depth + 1);
                } else if (linked instanceof TileBuilder builder) {
                    addComponent(new SnapshotComponent(builder.getBlueprintBuildingInfo()));
                } else if (linked instanceof TileConstructionMarker marker) {
                    addComponent(new SnapshotComponent(marker.getBlueprintBuildingInfo()));
                } else {
                    throw new PlanException("Composite endpoint " + linkedPos + " is no longer linkable");
                }
            }
        }

        private void addComponent(Component component) throws PlanException {
            if (components.size() >= MAX_COMPONENTS) {
                throw new PlanException("Composite blueprint exceeds " + MAX_COMPONENTS + " components");
            }
            components.add(component);
        }
    }

    public static class PlanException extends Exception {
        public PlanException(String message) {
            super(message);
        }

        public PlanException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
