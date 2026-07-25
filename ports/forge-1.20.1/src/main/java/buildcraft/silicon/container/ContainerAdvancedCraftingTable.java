/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import java.io.IOException;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotDisplay;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconGuis;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;

public class ContainerAdvancedCraftingTable extends ContainerBCTile<TileAdvancedCraftingTable> {
    private static final int MAX_RECIPE_ID_LENGTH = 256;
    private static final IdAllocator IDS = MenuBC_Neptune.IDS.makeChild("advanced_crafting_table");

    public static final int NET_SELECT_RECIPE = IDS.allocId("SELECT_RECIPE");

    public ContainerAdvancedCraftingTable(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
            containerId,
            playerInventory,
            new ItemHandlerSimple(15),
            new ItemHandlerSimple(9),
            new ItemHandlerSimple(9),
            new ItemHandlerSimple(1),
            createLevelAccess(playerInventory, buf)
        );
    }

    public ContainerAdvancedCraftingTable(
        int containerId,
        Inventory playerInventory,
        IItemHandlerAdv invMaterials,
        IItemHandlerAdv invResults,
        IItemHandlerAdv invBlueprint,
        IItemHandler clientResult,
        ContainerLevelAccess access
    ) {
        super(BCSiliconGuis.MENU_AD_CRAFTING_TABLE.get(), playerInventory, containerId, access);

        addSlot(new SlotDisplay(clientResult, 0, 127, 33));

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(invBlueprint, x + y * 3, 33 + x * 18, 16 + y * 18, false));
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                addSlot(new SlotBase(invMaterials, x + y * 5, 15 + x * 18, 85 + y * 18));
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotOutput(invResults, x + y * 3, 109 + x * 18, 85 + y * 18));
            }
        }
        addFullPlayerInventory(153);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    /**
     * Sends only the selected recipe identity. Ingredient stacks are resolved
     * from the authoritative server recipe manager and never trusted from the
     * client.
     */
    public void sendSelectRecipe(CraftingRecipe recipe) {
        if (!AdvancedCraftingRecipeSelection.isRepresentable(recipe)) {
            return;
        }
        sendMessage(NET_SELECT_RECIPE, buffer -> buffer.writeUtf(recipe.getId().toString(), MAX_RECIPE_ID_LENGTH));
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx)
        throws IOException {
        super.readMessage(id, buffer, side, ctx);
        if (side != LogicalSide.SERVER || id != NET_SELECT_RECIPE) {
            return;
        }

        ResourceLocation recipeId = ResourceLocation.tryParse(buffer.readUtf(MAX_RECIPE_ID_LENGTH));
        if (recipeId != null) {
            trySelectRecipe(ctx.getSender(), recipeId);
        }
    }

    boolean trySelectRecipe(ServerPlayer player, ResourceLocation recipeId) {
        if (player == null || tile == null || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        if (tile.getLevel() == null || tile.getLevel().isClientSide || player.level() != tile.getLevel()) {
            return false;
        }

        Recipe<?> found = tile.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
        if (!(found instanceof CraftingRecipe recipe)
            || !player.getRecipeBook().contains(recipe)
            || !AdvancedCraftingRecipeSelection.isRepresentable(recipe)) {
            return false;
        }
        return tile.applyRecipeSelection(recipe);
    }
}
