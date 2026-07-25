package buildcraft.lib.gui.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuiRecipeBookPhantomTest {
    @Test
    void installsItsPhantomRecipePageAgainstTheMapped1201Client() {
        GuiRecipeBookPhantom book = assertDoesNotThrow(
            () -> new GuiRecipeBookPhantom(recipe -> {
            })
        );
        assertTrue(book.hasInstalledPhantomPage());
    }
}
