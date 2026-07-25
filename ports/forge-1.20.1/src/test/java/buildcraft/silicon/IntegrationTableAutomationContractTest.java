package buildcraft.silicon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IntegrationTableAutomationContractTest {
    private static final Path SOURCE = Path.of(
        "src/main/java/buildcraft/silicon/tile/TileIntegrationTable.java"
    );

    @Test
    void resultInventoryAllowsExtractionButRejectsAutomationInsertion() throws IOException {
        String source = Files.readString(SOURCE);
        String resultRegistration = source.substring(
            source.indexOf("public final ItemHandlerSimple invResult"),
            source.indexOf("public final ItemProvider invOutput")
        );

        assertTrue(resultRegistration.contains("ItemHandlerManager.EnumAccess.EXTRACT"));
        assertFalse(resultRegistration.contains("ItemHandlerManager.EnumAccess.INSERT"));
        assertFalse(resultRegistration.contains("ItemHandlerManager.EnumAccess.BOTH"));
    }

    @Test
    void completedCraftChargesThePreMutationRecipeTargetExactlyOnce() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("long requiredPower = getTarget();"));
        assertTrue(source.contains("power >= requiredPower"));
        assertTrue(source.contains("power -= requiredPower;"));
        assertFalse(source.contains("power -= getTarget();"));
        assertTrue(source.contains(
            "if (extract(recipe.getCenterStack(), recipe.getRequirements(output), false))"
        ));
    }
}
