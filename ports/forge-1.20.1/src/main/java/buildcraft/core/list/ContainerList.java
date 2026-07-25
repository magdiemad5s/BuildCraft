/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.list;

import java.io.IOException;

import javax.annotation.Nonnull;

import buildcraft.api.lists.ListMatchHandler;
import buildcraft.core.BCCore;
import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemList_BC8;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.gui.widget.WidgetPhantomSlot;
import buildcraft.lib.list.ListHandler;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.data.IdAllocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class ContainerList extends MenuBC_Neptune {
    // Network ID's

    protected static final IdAllocator IDS = MenuBC_Neptune.IDS.makeChild("list");
    private static final int ID_LABEL = IDS.allocId("LABEL");
    private static final int ID_BUTTON = IDS.allocId("BUTTON");

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    // Main container list

    public ListHandler.Line[] lines;

    final WidgetListSlot[][] slots;
    private final ItemStack openedListStack;

    class WidgetListSlot extends WidgetPhantomSlot {
        final int lineIndex, slotIndex;

        public WidgetListSlot(int lineIndex, int slotIndex) {
            super(ContainerList.this);
            this.lineIndex = lineIndex;
            this.slotIndex = slotIndex;
        }

        @Override
        protected void onSetStack() {
            ContainerList.this.setStack(lineIndex, slotIndex, getStack());
        }
    }

    public ContainerList(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getOpenedListStack(playerInventory, buffer));
    }

    public ContainerList(int containerId, Inventory playerInventory, ItemStack openedListStack) {
        super(playerInventory, BCCore.LIST_MENU.get(), containerId);
        this.openedListStack = validateOpenedListStack(playerInventory, openedListStack);

        lines = ListHandler.getLines(this.openedListStack);

        slots = new WidgetListSlot[lines.length][ListHandler.WIDTH];
        for (int line = 0; line < lines.length; line++) {
            for (int slot = 0; slot < ListHandler.WIDTH; slot++) {
                WidgetListSlot widget = new WidgetListSlot(line, slot);
                slots[line][slot] = addWidget(widget);
                widget.setStack(lines[line].getStack(slot), false);
            }
        }

        addFullPlayerInventory(103);
    }

    @Override
	public boolean stillValid(Player player) {
        return !openedListStack.isEmpty()
            && (player.getItemInHand(InteractionHand.MAIN_HAND) == openedListStack
                || player.getItemInHand(InteractionHand.OFF_HAND) == openedListStack);
    }

    @Nonnull
    public ItemStack getListItemStack() {
        return stillValid(playerInventory.player) ? openedListStack : StackUtil.EMPTY;
    }

    @Nonnull
    private static ItemStack getOpenedListStack(Inventory inventory, FriendlyByteBuf buffer) {
        if (buffer == null || !buffer.isReadable()) {
            return StackUtil.EMPTY;
        }
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return validateOpenedListStack(inventory, inventory.player.getItemInHand(hand));
    }

    @Nonnull
    private static ItemStack validateOpenedListStack(Inventory inventory, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemList_BC8)) {
            return StackUtil.EMPTY;
        }
        return inventory.player.getItemInHand(InteractionHand.MAIN_HAND) == stack
            || inventory.player.getItemInHand(InteractionHand.OFF_HAND) == stack
                ? stack
                : StackUtil.EMPTY;
    }

    void setStack(final int lineIndex, final int slotIndex, @Nonnull final ItemStack stack) {
        if (!ListMenuStatePolicy.isValidSlot(lineIndex, slotIndex, lines.length, ListHandler.WIDTH)
            || getListItemStack().isEmpty()) {
            return;
        }
        lines[lineIndex].setStack(slotIndex, stack);
        ListHandler.saveLines(getListItemStack(), lines);
    }

    public void switchButton(final int lineIndex, final int button) {
        if (!ListMenuStatePolicy.isValidButton(lineIndex, button, lines.length)
            || getListItemStack().isEmpty()) {
            return;
        }
        lines[lineIndex].toggleOption(button);

        if (playerInventory.player.level().isClientSide) {
            sendMessage(ID_BUTTON, (buffer) -> {
                buffer.writeByte(lineIndex);
                buffer.writeByte(button);
            });
        } else if (button == 1 || button == 2) {
            ListMatchHandler.Type type = lines[lineIndex].getSortingType();
            if (type == ListMatchHandler.Type.MATERIAL || type == ListMatchHandler.Type.TYPE) {
                WidgetListSlot[] widgetSlots = slots[lineIndex];
                for (int i = 1; i < widgetSlots.length; i++) {
                    widgetSlots[i].setStack(StackUtil.EMPTY, true);
                }
            }
        }

        ListHandler.saveLines(getListItemStack(), lines);
    }

    public void setLabel(final String text) {
        ItemStack list = getListItemStack();
        if (list.isEmpty()) {
            return;
        }
        String clean = ListMenuStatePolicy.sanitizeLabel(text);
        BCCoreItems.LIST.get().setLabelName(list, clean);

        if (playerInventory.player.level().isClientSide) {
            sendMessage(ID_LABEL, (buffer) -> buffer.writeUtf(clean));
        }
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readMessage(id, buffer, side, ctx);
        if (side == LogicalSide.SERVER && ctx.getSender() == playerInventory.player
            && stillValid(playerInventory.player)) {
            if (id == ID_BUTTON) {
                int lineIndex = buffer.readUnsignedByte();
                int button = buffer.readUnsignedByte();
                if (ListMenuStatePolicy.isValidButton(lineIndex, button, lines.length)) {
                    switchButton(lineIndex, button);
                }
            } else if (id == ID_LABEL) {
                setLabel(buffer.readUtf(ListMenuStatePolicy.MAX_LABEL_LENGTH));
            }
        }
    }
}
