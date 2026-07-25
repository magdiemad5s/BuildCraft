/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.core;

import java.util.function.Consumer;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.lib.item.LegacyItemMetadata;
import buildcraft.lib.recipe.LegacyRecipeIds;
import buildcraft.lib.recipe.NbtShapedRecipeBuilder;
import buildcraft.lib.recipe.NbtShapelessRecipeBuilder;
import net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.PartialNBTIngredient;

public class BCCoreRecipes extends RecipeProvider {
    public static void init() {
    }

    public BCCoreRecipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        new NbtShapedRecipeBuilder(BCCoreItems.getPaintbrushStack(null))
            .pattern(" iw")
            .pattern(" gi")
            .pattern("s  ")
            .define('i', Items.STRING)
            .define('s', Items.STICK)
            .define('g', BCCoreItems.GEAR_WOOD.get())
            .define('w', Blocks.WHITE_WOOL)
            .unlockedBy("has_" + BCCoreItems.GEAR_WOOD.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_WOOD.get()))
            .save(writer, new ResourceLocation(BCCore.MODID, "paintbrush"));

        CompoundTag cleanBrushTag = new CompoundTag();
        cleanBrushTag.putInt(LegacyItemMetadata.TAG_METADATA, 0);
        for (DyeColor colour : DyeColor.values()) {
            new NbtShapelessRecipeBuilder(BCCoreItems.getPaintbrushStack(colour))
                .requires(PartialNBTIngredient.of(BCCoreItems.PAINT_BRUSH.get(), cleanBrushTag))
                .requires(DyeItem.byColor(colour))
                .unlockedBy("has_" + BCCoreItems.PAINT_BRUSH.getId().getPath(),
                    TriggerInstance.hasItems(BCCoreItems.PAINT_BRUSH.get()))
                .group("paintbrush_colouring")
                .save(writer, new ResourceLocation(BCCore.MODID, "paintbrush/" + colour.getSerializedName()));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_WOOD.get())
            .pattern(" s ")
            .pattern("s s")
            .pattern(" s ")
            .define('s', Items.STICK)
            .unlockedBy("has_stick", TriggerInstance.hasItems(Items.STICK))
            .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_STONE.get())
            .pattern(" o ")
            .pattern("oio")
            .pattern(" o ")
            .define('o', ItemTags.STONE_TOOL_MATERIALS)
            .define('i', BCCoreItems.GEAR_WOOD.get())
            .unlockedBy("has_" + BCCoreItems.GEAR_WOOD.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_WOOD.get()))
            .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_IRON.get())
            .pattern(" o ")
            .pattern("oio")
            .pattern(" o ")
            .define('o', Items.IRON_INGOT)
            .define('i', BCCoreItems.GEAR_STONE.get())
            .unlockedBy("has_" + BCCoreItems.GEAR_STONE.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_STONE.get()))
            .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_GOLD.get())
            .pattern(" o ")
            .pattern("oio")
            .pattern(" o ")
            .define('o', Items.GOLD_INGOT)
            .define('i', BCCoreItems.GEAR_STONE.get())
            .unlockedBy("has_" + BCCoreItems.GEAR_STONE.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_STONE.get()))
            .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_DIAMOND.get())
            .pattern(" o ")
            .pattern("oio")
            .pattern(" o ")
            .define('o', Items.DIAMOND)
            .define('i', BCCoreItems.GEAR_GOLD.get())
            .unlockedBy("has_" + BCCoreItems.GEAR_GOLD.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_GOLD.get()))
            .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BCCoreItems.WRENCH.get())
            .pattern("I I")
            .pattern(" G ")
            .pattern(" I ")
            .define('I', Items.IRON_INGOT)
            .define('G', BCCoreItems.GEAR_STONE.get())
            .unlockedBy("has_" + BCCoreItems.GEAR_STONE.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_STONE.get()))
            .save(writer);

        new NbtShapedRecipeBuilder(BCCoreItems.getEngineStack(EnumEngineType.WOOD))
            .pattern("www")
            .pattern(" g ")
            .pattern("GpG")
            .define('w', ItemTags.PLANKS)
            .define('g', Tags.Items.GLASS_COLORLESS)
            .define('G', BCCoreItems.GEAR_WOOD.get())
            .define('p', Blocks.PISTON)
            .unlockedBy("has_" + BCCoreItems.GEAR_WOOD.getId().getPath(),
                TriggerInstance.hasItems(BCCoreItems.GEAR_WOOD.get()))
            .save(writer, LegacyRecipeIds.REDSTONE_ENGINE);
    }
}
