/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import buildcraft.api.recipes.IngredientStack;
import net.minecraft.world.item.ItemStack;

/**
 * Allocates counted ingredients across an inventory without consuming the same
 * item twice when ingredient definitions overlap.
 */
public final class IngredientStackAllocation {
    private IngredientStackAllocation() {
    }

    /**
     * @return the number of items to consume from every slot, or {@code null}
     *         when no complete allocation exists
     */
    @Nullable
    public static int[] find(
        int slotCount,
        IntFunction<ItemStack> stackProvider,
        Collection<IngredientStack> requirements,
        boolean exact
    ) {
        if (slotCount < 0 || stackProvider == null || requirements == null) {
            return null;
        }

        ItemStack[] stacks = new ItemStack[slotCount];
        long totalAvailable = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = stackProvider.apply(slot);
            if (stack == null) {
                return null;
            }
            stacks[slot] = stack;
            if (!stack.isEmpty()) {
                totalAvailable += stack.getCount();
            }
        }

        List<IngredientStack> countedRequirements = new ArrayList<>(requirements.size());
        long totalRequired = 0;
        for (IngredientStack requirement : requirements) {
            if (requirement == null || requirement.ingredient == null || requirement.count < 0) {
                return null;
            }
            if (requirement.count == 0) {
                continue;
            }
            countedRequirements.add(requirement);
            totalRequired += requirement.count;
        }

        if (totalRequired > totalAvailable) {
            return null;
        }
        if (totalRequired == 0) {
            return new int[slotCount];
        }

        int requirementCount = countedRequirements.size();
        int source = 0;
        int firstRequirement = 1;
        int firstSlot = firstRequirement + requirementCount;
        int sink = firstSlot + slotCount;
        long[][] residual = new long[sink + 1][sink + 1];

        for (int requirementIndex = 0; requirementIndex < requirementCount; requirementIndex++) {
            IngredientStack requirement = countedRequirements.get(requirementIndex);
            int requirementNode = firstRequirement + requirementIndex;
            residual[source][requirementNode] = requirement.count;
            for (int slot = 0; slot < slotCount; slot++) {
                ItemStack stack = stacks[slot];
                if (!stack.isEmpty() && requirement.ingredient.test(stack)) {
                    residual[requirementNode][firstSlot + slot]
                        = Math.min(requirement.count, stack.getCount());
                }
            }
        }
        for (int slot = 0; slot < slotCount; slot++) {
            residual[firstSlot + slot][sink] = stacks[slot].isEmpty() ? 0 : stacks[slot].getCount();
        }

        long allocated = 0;
        int[] parent = new int[residual.length];
        while (findAugmentingPath(residual, source, sink, parent)) {
            long amount = Long.MAX_VALUE;
            for (int node = sink; node != source; node = parent[node]) {
                amount = Math.min(amount, residual[parent[node]][node]);
            }
            for (int node = sink; node != source; node = parent[node]) {
                int previous = parent[node];
                residual[previous][node] -= amount;
                residual[node][previous] += amount;
            }
            allocated += amount;
        }
        if (allocated != totalRequired) {
            return null;
        }

        int[] consumption = new int[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            long remaining = residual[firstSlot + slot][sink];
            consumption[slot] = (int) (stacks[slot].getCount() - remaining);
        }
        if (exact) {
            for (int slot = 0; slot < slotCount; slot++) {
                if (!stacks[slot].isEmpty() && consumption[slot] == 0) {
                    return null;
                }
            }
        }
        return consumption;
    }

    private static boolean findAugmentingPath(long[][] residual, int source, int sink, int[] parent) {
        java.util.Arrays.fill(parent, -1);
        parent[source] = source;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty()) {
            int from = queue.removeFirst();
            for (int to = 0; to < residual.length; to++) {
                if (parent[to] != -1 || residual[from][to] <= 0) {
                    continue;
                }
                parent[to] = from;
                if (to == sink) {
                    return true;
                }
                queue.addLast(to);
            }
        }
        return false;
    }
}
