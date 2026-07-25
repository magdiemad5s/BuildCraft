/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.item;

import java.util.List;


import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.lib.misc.LocaleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ItemPipeHolder extends BlockItem implements IItemPipe {
    public final PipeDefinition definition;
//    private final String id;
    private String unlocalizedName;

    public ItemPipeHolder(PipeDefinition definition) {
        super(BCTransportBlocks.pipeHolder.get(), new Item.Properties());
        this.definition = definition;
        this.unlocalizedName = definition.identifier.toLanguageKey("pipe");
    }



    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }
    
    // BlockItem overrides these to point to the block

	@Override
	public UseAnim getUseAnimation(ItemStack p_41452_) {
		// TODO Auto-generated method stub
		return super.getUseAnimation(p_41452_);
	}


	
	

    // Misc usefulness

		@Override
	public Component getName(ItemStack p_41458_) {
		return Component.translatable(unlocalizedName);
	}
		
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        String tipName = "tip." + unlocalizedName;
        if (I18n.exists(tipName)) {
            tooltip.add(Component.translatable(tipName).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }
        if (definition.flowType == PipeApi.flowFluids) {
            PipeApi.FluidTransferInfo fti = PipeApi.getFluidTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeFluidFlow(fti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowPower) {
            PipeApi.PowerTransferInfo pti = PipeApi.getPowerTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeMjFlow(pti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowRf) {
            PipeApi.RedstoneFluxTransferInfo pti = PipeApi.getRfTransferInfo(definition);
            tooltip.add(Component.translatable("buildcraft.rf.flow.short", pti.transferPerTick));
        }
		super.appendHoverText(stack, world, tooltip, flag);
	}
	
	




	
}
