/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import buildcraft.api.lists.ListMatchHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class ListMatchHandlerTools extends ListMatchHandler {
    private static final Set<ToolAction> STANDARD_DIG_ACTIONS = Set.of(
        ToolActions.AXE_DIG,
        ToolActions.PICKAXE_DIG,
        ToolActions.SHOVEL_DIG,
        ToolActions.HOE_DIG,
        ToolActions.SWORD_DIG,
        ToolActions.SHEARS_DIG
    );

    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type != Type.TYPE) {
            return false;
        }
        Set<ToolAction> sourceActions = getDigActions(stack);
        Set<ToolAction> targetActions = getDigActions(target);
        if (sourceActions.isEmpty() || targetActions.isEmpty()) {
            return false;
        }
        if (precise && sourceActions.size() != targetActions.size()) {
            return false;
        }
        return targetActions.containsAll(sourceActions);
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        return type == Type.TYPE && !getDigActions(stack).isEmpty();
    }

    private static Set<ToolAction> getDigActions(ItemStack stack) {
        Set<ToolAction> actions = new LinkedHashSet<>();
        Set<ToolAction> registeredActions = new LinkedHashSet<>(STANDARD_DIG_ACTIONS);
        registeredActions.addAll(ToolAction.getActions());
        for (ToolAction action : registeredActions) {
            if (action.name().endsWith("_dig") && stack.canPerformAction(action)) {
                actions.add(action);
            }
        }
        return actions;
    }
}
