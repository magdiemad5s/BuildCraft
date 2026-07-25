package buildcraft.core.list;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ListMenuOpenContractTest {
    @Test
    void menuUsesTheExactOpeningHandAndAForgeBufferFactory() throws IOException {
        String itemSource = read("src/main/java/buildcraft/core/item/ItemList_BC8.java");
        String menuSource = read("src/main/java/buildcraft/core/list/ContainerList.java");
        String coreSource = read("src/main/java/buildcraft/core/BCCore.java");

        assertTrue(itemSource.contains("buffer.writeEnum(hand)"));
        assertTrue(itemSource.contains("new ContainerList(containerId, inventory, openedStack)"));
        assertFalse(itemSource.contains("NetworkHooks.openScreen(sPlayer, this)"));

        assertTrue(menuSource.contains("buffer.readEnum(InteractionHand.class)"));
        assertTrue(menuSource.contains("== openedListStack"));
        assertTrue(coreSource.contains("BCContainerFactory.create(ContainerList::new)"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
