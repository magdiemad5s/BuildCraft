package buildcraft.lib.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideIndexTest {
    private static final GuideDocument WOOD_ITEM_PIPE = document(
        "buildcrafttransport:pipe/wood_item",
        "Extracts items from an inventory."
    );
    private static final GuideDocument TANK = document(
        "buildcraftfactory:block/tank",
        "Stores fluids in a vertical column."
    );
    private static final GuideDocument WRENCH = document(
        "buildcraftcore:item/wrench",
        "Rotates BuildCraft blocks."
    );
    private static final GuideDocument CONFIG = document(
        "buildcraftlib:config/guide_page_format",
        "Guide page authoring reference."
    );

    @Test
    void searchesTitlesContentAndTargetsCaseInsensitivelyWithABoundedLimit() {
        GuideIndex index = new GuideIndex(List.of(TANK, CONFIG, WRENCH, WOOD_ITEM_PIPE));

        assertEquals(List.of(WOOD_ITEM_PIPE), index.search(
            GuideContracts.DEFAULT_BOOK,
            "EXTRACTS ITEMS",
            10
        ));
        assertEquals(List.of(TANK), index.search(
            GuideContracts.DEFAULT_BOOK,
            "vertical column",
            10
        ));
        assertEquals(1, index.search(GuideContracts.DEFAULT_BOOK, "", 0).size());
        assertEquals(List.of(CONFIG), index.search(GuideContracts.META_BOOK, "", 100));
    }

    @Test
    void resolvesCanonicalIdsLegacyItemIdsResourcePathsFragmentsAndBackslashes() {
        GuideIndex index = new GuideIndex(List.of(TANK, CONFIG, WRENCH, WOOD_ITEM_PIPE));

        assertSame(WOOD_ITEM_PIPE, index.lookup("buildcrafttransport:pipe/wood_item"));
        assertSame(WOOD_ITEM_PIPE, index.lookup("BUILDCRAFTTRANSPORT:PIPE_WOOD_ITEM#connections"));
        assertSame(
            WOOD_ITEM_PIPE,
            index.lookup(
                "buildcrafttransport:compat/buildcraft/guide/en_us/pipe/wood_item.md#extracting"
            )
        );
        assertSame(
            WOOD_ITEM_PIPE,
            index.lookup(
                "buildcrafttransport:compat\\buildcraft\\guide\\en_us\\pipe\\wood_item.md"
            )
        );
        assertSame(TANK, index.lookup("buildcraftfactory:tank"));
        assertSame(WRENCH, index.lookup("wrench.md#usage"));
        assertNull(index.lookup("buildcraftfactory:block/missing"));
        assertNull(index.lookup(null));
    }

    @Test
    void refusesAnAmbiguousUnnamespacedLegacyPath() {
        GuideDocument otherWrench = document(
            "example:item/wrench",
            "A different wrench."
        );
        GuideIndex index = new GuideIndex(List.of(WRENCH, otherWrench));

        assertNull(index.lookup("wrench"));
        assertSame(WRENCH, index.lookup("buildcraftcore:wrench"));
    }

    private static GuideDocument document(String id, String prose) {
        return GuideDocumentParser.parse(id, prose, false);
    }
}
