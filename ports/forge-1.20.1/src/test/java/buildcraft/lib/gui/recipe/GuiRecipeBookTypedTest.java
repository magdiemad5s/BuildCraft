package buildcraft.lib.gui.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class GuiRecipeBookTypedTest {
    @Test
    void constructorUsesVanillaRecipeBookInitialization() {
        assertDoesNotThrow(GuiRecipeBookTyped::new);
    }
}
