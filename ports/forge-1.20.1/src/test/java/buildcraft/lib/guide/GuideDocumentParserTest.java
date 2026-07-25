package buildcraft.lib.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideDocumentParserTest {
    @Test
    void retainsLiteralPercentSignsAndFormatLikeTextAsProse() {
        String prose = "Runs at 75% capacity; placeholders such as %0 and ${domain}:%0 stay literal.";

        GuideDocument document = GuideDocumentParser.parse(
            "buildcraftcore:trigger/fluid_below_75",
            prose,
            false
        );

        assertEquals(prose, document.lines().get(0).text());
        assertTrue(document.searchText().contains("75%"));
        assertTrue(document.searchText().contains("%0"));
    }

    @Test
    void selectsTheDetailedAndCompactLoreBranches() {
        String source = """
            <lore>Detailed lore at 75% capacity.</lore>
            <no_lore>Compact lore at 50% capacity.</no_lore>
            <no_detail>Compact detail at 25% capacity.</no_detail>
            Always visible.
            """;

        GuideDocument detailed = GuideDocumentParser.parse(
            "buildcraftcore:trigger/fluid_below_75",
            source,
            true
        );
        GuideDocument compact = GuideDocumentParser.parse(
            "buildcraftcore:trigger/fluid_below_75",
            source,
            false
        );

        assertTrue(hasText(detailed, "Detailed lore at 75% capacity."));
        assertFalse(hasText(detailed, "Compact lore at 50% capacity."));
        assertFalse(hasText(detailed, "Compact detail at 25% capacity."));
        assertTrue(hasText(detailed, "Always visible."));

        assertFalse(hasText(compact, "Detailed lore at 75% capacity."));
        assertTrue(hasText(compact, "Compact lore at 50% capacity."));
        assertTrue(hasText(compact, "Compact detail at 25% capacity."));
        assertTrue(hasText(compact, "Always visible."));
    }

    @Test
    void convertsLegacyLinkRecipeImageAndPageDirectivesWithoutLosingTargets() {
        String source = """
            <link to="buildcraftcore:item/wrench"/>
            <recipe stack="buildcraftcore:gear_stone"/>
            <recipes stack="buildcraftcore:gear_wood"/>
            <usages stack="buildcraftcore:engine"/>
            <recipes_usages stack="buildcrafttransport:pipe_structure"/>
            <image src="buildcraftcore:items/wrench"/>
            <new_page/>
            $[special.new_page]
            $[special.all_crafting](buildcraftlib:guide)
            """;

        List<GuideLine> lines = GuideDocumentParser.parse(
            "buildcraftlib:config/guide_page_format",
            source,
            false
        ).lines().stream()
            .filter(line -> line.style() != GuideLine.Style.BLANK)
            .toList();

        assertEquals(
            new GuideLine(
                GuideLine.Style.LINK,
                "See also: Wrench",
                "buildcraftcore:item/wrench"
            ),
            lines.get(0)
        );
        assertEquals(
            new GuideLine(
                GuideLine.Style.RECIPE,
                "Recipe: Gear Stone",
                "buildcraftcore:gear_stone"
            ),
            lines.get(1)
        );
        assertEquals(
            new GuideLine(
                GuideLine.Style.RECIPE,
                "Recipes: Gear Wood",
                "buildcraftcore:gear_wood"
            ),
            lines.get(2)
        );
        assertEquals(
            new GuideLine(
                GuideLine.Style.RECIPE,
                "Uses: Engine",
                "buildcraftcore:engine"
            ),
            lines.get(3)
        );
        assertEquals(
            new GuideLine(
                GuideLine.Style.RECIPE,
                "Recipes and uses: Structure",
                "buildcrafttransport:pipe_structure"
            ),
            lines.get(4)
        );
        assertEquals(
            new GuideLine(
                GuideLine.Style.IMAGE,
                "Image: Wrench",
                "buildcraftcore:items/wrench"
            ),
            lines.get(5)
        );
        assertEquals(GuideLine.Style.PAGE_BREAK, lines.get(6).style());
        assertEquals(GuideLine.Style.PAGE_BREAK, lines.get(7).style());
        assertEquals(
            new GuideLine(
                GuideLine.Style.RECIPE,
                "Recipes: Guide",
                "buildcraftlib:guide"
            ),
            lines.get(8)
        );
        assertEquals(9, lines.size());
    }

    @Test
    void decodesEscapedLineBreaksInProseButPreservesCodeExamples() {
        String source =
            "First line\\nSecond line\n"
                + "<guide_md>literal\\ncode</guide_md>\n"
                + "Third line\\nFourth line";

        List<GuideLine> lines = GuideDocumentParser.parse(
            "buildcraftlib:config/escaped_lines",
            source,
            false
        ).lines().stream()
            .filter(line -> line.style() != GuideLine.Style.BLANK)
            .toList();

        assertEquals(new GuideLine(GuideLine.Style.NORMAL, "First line"), lines.get(0));
        assertEquals(new GuideLine(GuideLine.Style.NORMAL, "Second line"), lines.get(1));
        assertEquals(new GuideLine(GuideLine.Style.CODE, "literal\\ncode"), lines.get(2));
        assertEquals(new GuideLine(GuideLine.Style.NORMAL, "Third line"), lines.get(3));
        assertEquals(new GuideLine(GuideLine.Style.NORMAL, "Fourth line"), lines.get(4));
        assertEquals(5, lines.size());
    }

    private static boolean hasText(GuideDocument document, String text) {
        return document.lines().stream().anyMatch(line -> line.text().equals(text));
    }
}
