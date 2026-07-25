/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.tile.craft;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

/**
 * Server-side crafting state shared by BuildCraft's automated workbenches.
 *
 * <p>The phantom blueprint remains the persisted source of truth. A temporary
 * vanilla crafting grid is populated only while a craft is being executed so
 * recipe matching, dynamic outputs, and container remainders retain vanilla
 * behavior.</p>
 */
public class WorkbenchCrafting extends TransientCraftingContainer {
    enum EnumRecipeType {
        INGREDIENTS,
        EXACT_STACKS
    }

    public static final AbstractContainerMenu CONTAINER_EVENT_HANDLER = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    };

    private final TileBC_Neptune tile;
    private final ItemHandlerSimple invBlueprint;
    private final ItemHandlerSimple invMaterials;
    private final ItemHandlerSimple invResult;
    private final ItemHandlerSimple invAssumedResult = new ItemHandlerSimple(1);
    private final int craftTableSize;

    private boolean isBlueprintDirty = true;
    private boolean areMaterialsDirty = true;
    private boolean cachedHasRequirements;

    @Nullable
    private CraftingRecipe currentRecipe;
    private ItemStack assumedResult = ItemStack.EMPTY;
    @Nullable
    private EnumRecipeType recipeType;

    public WorkbenchCrafting(
        int width,
        int height,
        TileBC_Neptune tile,
        ItemHandlerSimple invBlueprint,
        ItemHandlerSimple invMaterials,
        ItemHandlerSimple invResult
    ) {
        super(CONTAINER_EVENT_HANDLER, width, height);
        this.tile = tile;
        this.invBlueprint = invBlueprint;
        this.invMaterials = invMaterials;
        this.invResult = invResult;
        craftTableSize = width * height;

        if (invBlueprint.getSlots() < craftTableSize) {
            throw new IllegalArgumentException(
                "Passed blueprint has a smaller size than width * height! (expected "
                    + craftTableSize + ", got " + invBlueprint.getSlots() + ")"
            );
        }
    }

    /**
     * Recipe lookup must see the phantom blueprint. Once lookup finishes,
     * crafting operations read the real temporary grid populated from stored
     * materials.
     */
    @Override
    public ItemStack getItem(int index) {
        if (isBlueprintDirty && index >= 0 && index < craftTableSize) {
            return invBlueprint.getStackInSlot(index);
        }
        return super.getItem(index);
    }

    public ItemStack getAssumedResult() {
        return assumedResult;
    }

    public int getSlotSize() {
        return invBlueprint.getSlots() + invMaterials.getSlots() + invResult.getSlots();
    }

    public void onInventoryChange(IItemHandler inventory) {
        if (inventory == invBlueprint) {
            isBlueprintDirty = true;
            areMaterialsDirty = true;
            cachedHasRequirements = false;
        } else if (inventory == invMaterials) {
            areMaterialsDirty = true;
            cachedHasRequirements = false;
        }
    }

    /**
     * Re-resolves a changed phantom blueprint.
     *
     * @return true when the assumed result changed or was recomputed
     */
    public boolean tick() {
        Level level = requireServerLevel();
        if (!isBlueprintDirty) {
            return false;
        }

        currentRecipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, this, level)
            .orElse(null);
        if (currentRecipe == null) {
            assumedResult = ItemStack.EMPTY;
            recipeType = null;
        } else {
            // Dynamic recipes derive their output from the blueprint.
            // getResultItem() is only a recipe-book display hint.
            assumedResult = currentRecipe.assemble(this, level.registryAccess());
            NonNullList<Ingredient> ingredients = currentRecipe.getIngredients();
            recipeType = ingredients.isEmpty() ? EnumRecipeType.EXACT_STACKS : EnumRecipeType.INGREDIENTS;
        }

        invAssumedResult.setStackInSlot(0, assumedResult.copy());
        isBlueprintDirty = false;
        areMaterialsDirty = true;
        cachedHasRequirements = false;
        return true;
    }

    /**
     * @return true if a call to {@link #craft()} can currently succeed
     */
    public boolean canCraft() {
        if (currentRecipe == null || recipeType == null || isBlueprintDirty || assumedResult.isEmpty()) {
            return false;
        }
        if (!invResult.canFullyAccept(assumedResult)) {
            return false;
        }
        if (areMaterialsDirty) {
            areMaterialsDirty = false;
            cachedHasRequirements = hasRequiredStacks();
        }
        return cachedHasRequirements;
    }

    /**
     * Attempts one craft. The server rechecks recipe matching after moving
     * actual material stacks into the temporary crafting grid.
     */
    public boolean craft() {
        if (currentRecipe == null || recipeType == null || isBlueprintDirty || !canCraft()) {
            return false;
        }
        return craftExact();
    }

    private boolean hasRequiredStacks() {
        List<ItemStack> required = collectRequiredStacks();
        if (required.isEmpty()) {
            return false;
        }

        List<ItemStack> available = new ArrayList<>(invMaterials.getSlots());
        for (int slot = 0; slot < invMaterials.getSlots(); slot++) {
            available.add(invMaterials.getStackInSlot(slot).copy());
        }

        for (ItemStack requiredStack : required) {
            int remaining = requiredStack.getCount();
            for (ItemStack candidate : available) {
                if (remaining <= 0) {
                    break;
                }
                if (!candidate.isEmpty() && matchesBlueprintStack(requiredStack, candidate)) {
                    int used = Math.min(remaining, candidate.getCount());
                    candidate.shrink(used);
                    remaining -= used;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> collectRequiredStacks() {
        List<ItemStack> required = new ArrayList<>(craftTableSize);
        for (int slot = 0; slot < craftTableSize; slot++) {
            ItemStack blueprintStack = invBlueprint.getStackInSlot(slot);
            if (blueprintStack.isEmpty()) {
                continue;
            }

            ItemStack matching = null;
            for (ItemStack existing : required) {
                if (StackUtil.canMerge(existing, blueprintStack)) {
                    matching = existing;
                    break;
                }
            }
            if (matching == null) {
                required.add(blueprintStack.copy());
            } else {
                matching.grow(blueprintStack.getCount());
            }
        }
        return required;
    }

    public static boolean matchesBlueprintStack(ItemStack blueprint, ItemStack candidate) {
        if (!ItemStack.isSameItem(blueprint, candidate)) {
            return false;
        }
        if (!blueprint.isDamageableItem()) {
            return ItemStack.isSameItemSameTags(blueprint, candidate);
        }
        ItemStack normalizedBlueprint = blueprint.copy();
        ItemStack normalizedCandidate = candidate.copy();
        normalizedBlueprint.setDamageValue(0);
        normalizedCandidate.setDamageValue(0);
        return ItemStack.isSameItemSameTags(normalizedBlueprint, normalizedCandidate);
    }

    private boolean craftExact() {
        Level level = requireServerLevel();
        BlockPos pos = tile.getBlockPos();

        // Recover from any interrupted previous attempt before extracting more.
        if (!clearTemporaryGrid()) {
            return false;
        }

        for (int slot = 0; slot < craftTableSize; slot++) {
            ItemStack blueprintStack = invBlueprint.getStackInSlot(slot);
            if (blueprintStack.isEmpty()) {
                continue;
            }

            ItemStack extracted = invMaterials.extract(
                candidate -> matchesBlueprintStack(blueprintStack, candidate),
                1,
                1,
                false
            );
            if (extracted.isEmpty()) {
                clearTemporaryGrid();
                return false;
            }
            super.setItem(slot, extracted);
        }

        // Some dynamic recipes cache data during matches() for assemble().
        if (!currentRecipe.matches(this, level)) {
            clearTemporaryGrid();
            return false;
        }
        ItemStack result = currentRecipe.assemble(this, level.registryAccess());
        if (result.isEmpty()) {
            clearTemporaryGrid();
            return false;
        }

        ItemStack leftover = invResult.insert(result, false, false);
        if (!leftover.isEmpty()) {
            InventoryUtil.addToBestAcceptor(level, pos, null, leftover);
        }

        NonNullList<ItemStack> remainingStacks = currentRecipe.getRemainingItems(this);
        for (int slot = 0; slot < craftTableSize; slot++) {
            ItemStack inSlot = super.getItem(slot);
            if (!inSlot.isEmpty()) {
                super.removeItem(slot, 1);
            }

            if (slot < remainingStacks.size()) {
                ItemStack remaining = remainingStacks.get(slot);
                if (!remaining.isEmpty()) {
                    leftover = invMaterials.insert(remaining, false, false);
                    if (!leftover.isEmpty()) {
                        InventoryUtil.addToBestAcceptor(level, pos, null, leftover);
                    }
                }
            }
        }

        // Return any recipe-specific unconsumed inputs to material storage.
        clearTemporaryGridToStorage(level, pos);
        areMaterialsDirty = true;
        cachedHasRequirements = false;
        return true;
    }

    /**
     * Moves the complete temporary grid back into material storage.
     *
     * @return false if storage cannot accept all temporary items
     */
    private boolean clearTemporaryGrid() {
        for (int slot = 0; slot < craftTableSize; slot++) {
            ItemStack inSlot = super.getItem(slot);
            if (inSlot.isEmpty()) {
                continue;
            }

            ItemStack leftover = invMaterials.insert(inSlot, false, false);
            int inserted = inSlot.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
            if (inserted > 0) {
                super.removeItem(slot, inserted);
            }
            if (!leftover.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void clearTemporaryGridToStorage(Level level, BlockPos pos) {
        for (int slot = 0; slot < craftTableSize; slot++) {
            ItemStack inSlot = super.removeItemNoUpdate(slot);
            if (inSlot.isEmpty()) {
                continue;
            }

            ItemStack leftover = invMaterials.insert(inSlot, false, false);
            if (!leftover.isEmpty()) {
                InventoryUtil.addToBestAcceptor(level, pos, null, leftover);
            }
        }
    }

    private Level requireServerLevel() {
        Level level = tile.getLevel();
        if (level == null) {
            throw new IllegalStateException("Workbench crafting is not attached to a level");
        }
        if (level.isClientSide) {
            throw new IllegalStateException("Never call workbench crafting logic on the client side");
        }
        return level;
    }

    public RecipeBookMenu<WorkbenchCrafting> getCraftingMenu(AbstractContainerMenu menu) {
        return new InnerRecipeBookMenu(menu);
    }

    protected class InnerRecipeBookMenu extends RecipeBookMenu<WorkbenchCrafting> {
        private final AbstractContainerMenu delegate;

        protected InnerRecipeBookMenu(AbstractContainerMenu menu) {
            super(menu.getType(), menu.containerId);
            delegate = menu;
            slots.addAll(menu.slots);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return delegate.stillValid(player);
        }

        @Override
        public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
            WorkbenchCrafting.this.fillStackedContents(stackedContents);
        }

        @Override
        public void clearCraftingContent() {
            WorkbenchCrafting.this.clearContent();
        }

        @Override
        public boolean recipeMatches(Recipe<? super WorkbenchCrafting> recipe) {
            return recipe.matches(WorkbenchCrafting.this, tile.getLevel());
        }

        @Override
        public int getResultSlotIndex() {
            return 0;
        }

        @Override
        public int getGridWidth() {
            return WorkbenchCrafting.this.getWidth();
        }

        @Override
        public int getGridHeight() {
            return WorkbenchCrafting.this.getHeight();
        }

        @Override
        public int getSize() {
            return craftTableSize + 1;
        }

        @Override
        public RecipeBookType getRecipeBookType() {
            return RecipeBookType.CRAFTING;
        }

        @Override
        public boolean shouldMoveToInventory(int slot) {
            return false;
        }

        @Override
        public Slot getSlot(int slot) {
            return delegate.getSlot(slot);
        }
    }
}
