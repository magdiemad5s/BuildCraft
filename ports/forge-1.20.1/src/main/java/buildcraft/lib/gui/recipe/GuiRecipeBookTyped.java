package buildcraft.lib.gui.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.item.crafting.Recipe;

/** A {@link RecipeBookComponent} that is for recipes in a different type than {@link Recipe}. */
public class GuiRecipeBookTyped extends RecipeBookComponent {
    public GuiRecipeBookTyped() {
        /*
         * RecipeBookComponent is concrete in 1.20.1 and already operates on
         * Recipe<?>. Vanilla crafting screens construct it directly, so the
         * modern compatibility wrapper only needs the vanilla initialization.
         */
        super();
    }
}
