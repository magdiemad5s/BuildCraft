package buildcraft.silicon.container;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AdvancedCraftingRecipeSelectionTest {
    @Test
    void centersOneByOneRecipesLikeTheReleasedGui() {
        assertArrayEquals(
            new int[] {-1, -1, -1, -1, 0, -1, -1, -1, -1},
            AdvancedCraftingRecipeSelection.layoutIngredientIndexes(1, 1, 1)
        );
    }

    @Test
    void centersOnlyTheNarrowAxisForOneByTwoAndTwoByOneRecipes() {
        assertArrayEquals(
            new int[] {-1, 0, -1, -1, 1, -1, -1, -1, -1},
            AdvancedCraftingRecipeSelection.layoutIngredientIndexes(1, 2, 2)
        );
        assertArrayEquals(
            new int[] {-1, -1, -1, 0, 1, -1, -1, -1, -1},
            AdvancedCraftingRecipeSelection.layoutIngredientIndexes(2, 1, 2)
        );
    }

    @Test
    void laysShapelessIngredientsOutInLegacyRowOrder() {
        assertArrayEquals(
            new int[] {0, 1, 2, 3, -1, -1, -1, -1, -1},
            AdvancedCraftingRecipeSelection.layoutIngredientIndexes(3, 3, 4)
        );
    }

    @Test
    void rejectsLayoutsThatCannotFitTheLegacyGrid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AdvancedCraftingRecipeSelection.layoutIngredientIndexes(4, 1, 1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AdvancedCraftingRecipeSelection.layoutIngredientIndexes(2, 2, 5)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AdvancedCraftingRecipeSelection.layoutIngredientIndexes(1, 1, 0)
        );
    }
}
