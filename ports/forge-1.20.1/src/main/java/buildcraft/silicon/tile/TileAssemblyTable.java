/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import buildcraft.api.core.EnumPipePart;
import buildcraft.lib.gui.ItemProvider;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageUpdateTile;
import buildcraft.lib.recipe.AssemblyRecipeBasic;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerManager;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.AssemblyTableStatePolicy;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconRecipes;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.container.ContainerAssemblyTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class TileAssemblyTable extends TileLaserTableBase implements MenuProvider{
    public static final IdAllocator IDS = TileBC_Neptune.IDS.makeChild("assembly_table");
    public static final int NET_RECIPE_STATE = IDS.allocId("RECIPE_STATE");

    public final ItemHandlerSimple inv = itemManager.addInvHandler(
        "inv",
        3 * 4,
        ItemHandlerManager.EnumAccess.BOTH,
        EnumPipePart.VALUES
    );
    public SortedMap<AssemblyInstruction, EnumAssemblyRecipeState> recipesStates = new TreeMap<>();
    public final ItemProvider display = new ItemProvider((i) -> {
    	return i < recipesStates.size() ? new ArrayList<>(recipesStates.keySet()).get(i).output : ItemStack.EMPTY;}, 3 * 4);

    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftsilicon:precision_crafting");
    
    protected boolean isDirty = true;
    private final List<PendingRecipeState> pendingRecipeStates = new ArrayList<>();

    public TileAssemblyTable(BlockPos pos, BlockState state) {
    	super(BCSiliconBlocks.ASSEMBLY_TABLE_TILE.get(), pos, state);
    	inv.setCallback((a,b,c,d) -> isDirty = true);
    }
    
    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    private void updateRecipes() {
        SortedMap<AssemblyInstruction, EnumAssemblyRecipeState> previousStates =
            new TreeMap<>(recipesStates);
        if(isDirty) {
	        for(AssemblyRecipeBasic recipe: level.getRecipeManager().getAllRecipesFor(BCSiliconRecipes.ASSEMBLY_TYPE.get())) {
	            Set<ItemStack> outputs = recipe.getOutputs(inv);
	            for (ItemStack out: outputs) {
	            	if(out.isEmpty())
	            		break;
	                boolean found = false;
	                for (AssemblyInstruction instruction: recipesStates.keySet()) {
	                    if (instruction.recipe == recipe && ItemStack.matches(out, instruction.output)) {
	                        found = true;
	                        break;
	                    }
	                }
	                AssemblyInstruction instruction = new AssemblyInstruction(recipe, out);
	                if (!found && !recipesStates.containsKey(instruction)
                    && recipesStates.size() < AssemblyTableStatePolicy.MAX_SYNCED_RECIPES) {
	                    recipesStates.put(instruction, EnumAssemblyRecipeState.POSSIBLE);
	                }
	            }
	        }
	        isDirty = false;
        }

        boolean findActive = false;
        for (Iterator<Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState>> iterator = recipesStates.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry = iterator.next();
            AssemblyInstruction instruction = entry.getKey();
            EnumAssemblyRecipeState state = entry.getValue();
            boolean enough = extract(inv, instruction.recipe.getInputsFor(instruction.output), true, false);
            if (state == EnumAssemblyRecipeState.POSSIBLE) {
                if (!enough) {
                    iterator.remove();
                }
            } else {
                if (enough) {
                    if (state == EnumAssemblyRecipeState.SAVED) {
                        state = EnumAssemblyRecipeState.SAVED_ENOUGH;
                    }
                } else {
                    if (state != EnumAssemblyRecipeState.SAVED) {
                        state = EnumAssemblyRecipeState.SAVED;
                    }
                }
            }
            if (state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                findActive = true;
            }
            entry.setValue(state);
        }
        if (!findActive) {
            for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
                EnumAssemblyRecipeState state = entry.getValue();
                if (state == EnumAssemblyRecipeState.SAVED_ENOUGH) {
                    state = EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE;
                    entry.setValue(state);
                    break;
                }
            }
        }
        if (AssemblyTableStatePolicy.hasRecipeStateSnapshotChanged(previousStates, recipesStates)) {
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    private AssemblyInstruction getActiveRecipe() {
        return recipesStates.entrySet().stream().filter(entry -> entry.getValue() == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private void activateNextRecipe() {
        AssemblyInstruction activeRecipe = getActiveRecipe();
        if (activeRecipe != null) {
            int index = 0;
            int activeIndex = 0;
            boolean isActiveLast = false;
            long enoughCount = recipesStates.values().stream().filter(state -> state == EnumAssemblyRecipeState.SAVED_ENOUGH || state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE).count();
            if (enoughCount <= 1) {
                return;
            }
            for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
                EnumAssemblyRecipeState state = entry.getValue();
                if (state == EnumAssemblyRecipeState.SAVED_ENOUGH) {
                    isActiveLast = false;
                }
                if (state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                    state = EnumAssemblyRecipeState.SAVED_ENOUGH;
                    entry.setValue(state);
                    activeIndex = index;
                    isActiveLast = true;
                }
                index++;
            }
            index = 0;
            for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
            	AssemblyRecipeBasic recipe = entry.getKey().recipe;
                EnumAssemblyRecipeState state = entry.getValue();
                if (state == EnumAssemblyRecipeState.SAVED_ENOUGH && recipe != activeRecipe.recipe && (index > activeIndex || isActiveLast)) {
                    state = EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE;
                    entry.setValue(state);
                    break;
                }
                index++;
            }
        }
    }

    @Override
    public long getTarget() {
        return Optional.ofNullable(getActiveRecipe()).map(instruction -> instruction.recipe.getRequiredMicroJoulesFor(instruction.output)).orElse(0L);
    }

    @Override
    public void update() {
        super.update();

        if (level.isClientSide) {

            return;
        }
    	
 //        if(isDirty)
        updateRecipes();


        var knownOwner = getKnownOwner();
        if (getTarget() > 0) {
            if (knownOwner != null && knownOwner.getId() != null) {
                AdvancementUtil.unlockAdvancement(knownOwner.getId(), ADVANCEMENT);
            }
            if (power >= getTarget()) {
                AssemblyInstruction instruction = getActiveRecipe();
                extract(inv, instruction.recipe.getInputsFor(instruction.output), false, false);

                InventoryUtil.addToBestAcceptor(getLevel(), getBlockPos(), null, instruction.output.copy());

                power -= getTarget();
                activateNextRecipe();
            }
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    @Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
	      ListTag recipesStatesTag = new ListTag();
	        recipesStates.forEach((instruction, state) -> {
	            CompoundTag entryTag = new CompoundTag();
	            entryTag.putString("recipe", instruction.recipe.getId().toString());
	            entryTag.put("output", instruction.output.serializeNBT());
	            entryTag.putInt("state", state.ordinal());
	            recipesStatesTag.add(entryTag);
	        });
	        nbt.put("recipes_states", recipesStatesTag);
	}
    
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		recipesStates.clear();
        pendingRecipeStates.clear();
        isDirty = true;
		ListTag recipesStatesTag = nbt.getList("recipes_states", Tag.TAG_COMPOUND);
        int count = Math.min(recipesStatesTag.size(), AssemblyTableStatePolicy.MAX_SYNCED_RECIPES);
        for (int i = 0; i < count; i++) {
            CompoundTag entryTag = recipesStatesTag.getCompound(i);
            EnumAssemblyRecipeState state = AssemblyTableStatePolicy.stateByOrdinal(entryTag.getInt("state"));
            if (entryTag.contains("output", Tag.TAG_COMPOUND) && state != null) {
                pendingRecipeStates.add(new PendingRecipeState(
                    entryTag.getString("recipe"),
                    ItemStack.of(entryTag.getCompound("output")),
                    state
                ));
            }
        }
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
        for (PendingRecipeState pending : pendingRecipeStates) {
            AssemblyInstruction instruction = lookupRecipe(pending.recipeId(), pending.output());
            if (instruction != null) {
                recipesStates.put(instruction, pending.state());
            }
        }
        pendingRecipeStates.clear();
	}

	@Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);

        if (id == NET_GUI_DATA) {
            int count = Math.min(recipesStates.size(), AssemblyTableStatePolicy.MAX_SYNCED_RECIPES);
            buffer.writeInt(count);
            int written = 0;
            for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
                if (written++ >= count) {
                    break;
                }
                AssemblyInstruction instruction = entry.getKey();
                buffer.writeUtf(instruction.recipe.getId().toString());
                buffer.writeItem(instruction.output);
                buffer.writeInt(entry.getValue().ordinal());
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);

        if (id == NET_GUI_DATA) {
            if (side != LogicalSide.CLIENT) {
                throw new IOException("Rejected serverbound Assembly Table recipe-state snapshot");
            }
            int count = buffer.readInt();
            if (!AssemblyTableStatePolicy.isValidRecipeCount(count)) {
                throw new IOException("Invalid Assembly Table recipe count: " + count);
            }
            SortedMap<AssemblyInstruction, EnumAssemblyRecipeState> decoded = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                String recipeId = buffer.readUtf(AssemblyTableStatePolicy.MAX_RECIPE_ID_LENGTH);
                ItemStack output = buffer.readItem();
                EnumAssemblyRecipeState state = AssemblyTableStatePolicy.stateByOrdinal(buffer.readInt());
                if (state == null) {
                    throw new IOException("Invalid Assembly Table recipe state");
                }
                AssemblyInstruction instruction = lookupRecipe(recipeId, output);
                if (instruction != null) {
                    decoded.put(instruction, state);
                }
            }
            recipesStates.clear();
            recipesStates.putAll(decoded);
        } else if (id == NET_RECIPE_STATE) {
            if (side != LogicalSide.SERVER) {
                throw new IOException("Rejected clientbound Assembly Table recipe-state request");
            }
            String recipeId = buffer.readUtf(AssemblyTableStatePolicy.MAX_RECIPE_ID_LENGTH);
            ItemStack output = buffer.readItem();
            EnumAssemblyRecipeState state = AssemblyTableStatePolicy.stateByOrdinal(buffer.readInt());
            if (state == null || !AssemblyTableStatePolicy.isClientSelectable(state)) {
                throw new IOException("Invalid client-selected Assembly Table recipe state");
            }
            AssemblyInstruction recipe = lookupRecipe(recipeId, output);
            if (recipe != null && recipesStates.containsKey(recipe)) {
                recipesStates.put(recipe, state);
                markChunkDirty();
                sendNetworkUpdate(NET_GUI_DATA);
            }
        }
    }

    public void sendRecipeStateToServer(AssemblyInstruction instruction, EnumAssemblyRecipeState state) {
        if (!AssemblyTableStatePolicy.isClientSelectable(state)) {
            return;
        }
    	MessageUpdateTile message = createMessage(NET_RECIPE_STATE, (buffer) -> {
            buffer.writeUtf(instruction.recipe.getId().toString());
            buffer.writeItem(instruction.output);
            buffer.writeInt(state.ordinal());
        });
        MessageManager.sendToServer(message);
    }
    
	@Override
	public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
		if(player instanceof ServerPlayer splayer&&!player.level().isClientSide) {
			NetworkHooks.openScreen(splayer, this, worldPosition);
		}
		return super.onActivated(player, hand, hit);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p_39956_) {
		return new ContainerAssemblyTable(id, inventory, inv, display, ContainerLevelAccess.create(level, worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        super.getDebugInfo(left, right, side);
        left.add("recipes - " + recipesStates.size());
        left.add("target - " + LocaleUtil.localizeMj(getTarget()));
    }
    
    @Nullable
    private AssemblyInstruction lookupRecipe(String name, ItemStack output) {
        if (level == null || name == null || name.isBlank() || name.length() > AssemblyTableStatePolicy.MAX_RECIPE_ID_LENGTH) {
            return null;
        }
        ResourceLocation recipeId = ResourceLocation.tryParse(name);
        if (recipeId == null) {
            return null;
        }
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(recipeId);
        return (recipe.isPresent() && recipe.get() instanceof AssemblyRecipeBasic assemblyRecipeBasic)
        		? new AssemblyInstruction(assemblyRecipeBasic, output) : null;
    }

    public class AssemblyInstruction implements Comparable<AssemblyInstruction> {
        public final AssemblyRecipeBasic recipe;
        public final ItemStack output;

        private AssemblyInstruction(AssemblyRecipeBasic recipe, ItemStack output) {
            this.recipe = recipe;
            this.output = output;
        }

        @Override
        public int compareTo(AssemblyInstruction o) {
            int recipeOrder = recipe.getId().compareTo(o.recipe.getId());
            return recipeOrder != 0
                ? recipeOrder
                : output.serializeNBT().toString().compareTo(o.output.serializeNBT().toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AssemblyInstruction)) return false;
            AssemblyInstruction instruction = (AssemblyInstruction) obj;
            return recipe.getId().equals(instruction.recipe.getId()) && ItemStack.matches(output, instruction.output);
        }

        @Override
        public int hashCode() {
            return 31 * recipe.getId().hashCode() + output.serializeNBT().hashCode();
        }
    }

    private record PendingRecipeState(
        String recipeId,
        ItemStack output,
        EnumAssemblyRecipeState state
    ) {}

}
