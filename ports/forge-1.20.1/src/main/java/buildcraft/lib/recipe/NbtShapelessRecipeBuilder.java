package buildcraft.lib.recipe;

import java.util.function.Consumer;

import com.google.gson.JsonObject;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Forge-compatible shapeless recipe builder that retains result-stack NBT. */
public final class NbtShapelessRecipeBuilder extends ShapelessRecipeBuilder {
    private final CompoundTag resultTag;

    public NbtShapelessRecipeBuilder(ItemStack result) {
        super(RecipeCategory.MISC, result.getItem(), result.getCount());
        this.resultTag = result.getOrCreateTag().copy();
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        super.save(result -> consumer.accept(new Result(resultTag, result)), id);
    }

    private record Result(CompoundTag tag, FinishedRecipe delegate) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            delegate.serializeRecipeData(json);
            json.getAsJsonObject("result").addProperty("nbt", tag.toString());
        }

        @Override
        public ResourceLocation getId() {
            return delegate.getId();
        }

        @Override
        public RecipeSerializer<?> getType() {
            return delegate.getType();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return delegate.serializeAdvancement();
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return delegate.getAdvancementId();
        }
    }
}
