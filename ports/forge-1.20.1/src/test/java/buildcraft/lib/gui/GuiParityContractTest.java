/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GuiParityContractTest {
    private static final Path JAVA_ROOT = Path.of("src/main/java/buildcraft");
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets");

    private static final Pattern SINGLE_ARGUMENT_RESOURCE = Pattern.compile(
        "new\\s+ResourceLocation\\(\\s*\"(buildcraft[a-z]*):"
            + "((?:textures/gui/[^\"\\r\\n]+)|(?:gui/[^\"\\r\\n]+\\.json))\"\\s*\\)"
    );
    private static final Pattern TWO_ARGUMENT_RESOURCE = Pattern.compile(
        "new\\s+ResourceLocation\\(\\s*\"(buildcraft[a-z]*)\"\\s*,\\s*"
            + "\"((?:textures/gui/[^\"\\r\\n]+)|(?:gui/[^\"\\r\\n]+\\.json))\"\\s*\\)"
    );

    private static final List<ModuleMenus> MODULE_MENUS = List.of(
        new ModuleMenus(
            "src/main/java/buildcraft/core/BCCore.java",
            "src/main/java/buildcraft/core/BCCore.java",
            List.of("LIST_MENU")
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/builders/BCBuildersGuis.java",
            "src/main/java/buildcraft/builders/BCBuildersClientGuis.java",
            List.of(
                "BCBuildersGuis.MENU_ARCHITECT_TABLE",
                "BCBuildersGuis.MENU_BUILDER",
                "BCBuildersGuis.MENU_ELIBRARY",
                "BCBuildersGuis.MENU_FILLER",
                "BCBuildersGuis.MENU_FILLER_PLANNER",
                "BCBuildersGuis.MENU_REPLACER"
            )
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/energy/BCEnergyGuis.java",
            "src/main/java/buildcraft/energy/BCEnergyClientProxy.java",
            List.of(
                "BCEnergyGuis.MENU_STONE",
                "BCEnergyGuis.MENU_IRON",
                "BCEnergyGuis.MENU_RF",
                "BCEnergyGuis.DYNAMO_MJ"
            )
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/factory/BCFactoryGuis.java",
            "src/main/java/buildcraft/factory/BCFactoryClientGuis.java",
            List.of(
                "BCFactoryGuis.MENU_AUTOWORK_BENCH_ITEM",
                "BCFactoryGuis.MENU_HEAT_EXCHANGE",
                "BCFactoryGuis.MENU_CHUTE",
                "BCFactoryGuis.MENU_TANK",
                "BCFactoryGuis.MENU_DISTILLER"
            )
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/silicon/BCSiliconGuis.java",
            "src/main/java/buildcraft/silicon/BCSiliconClientGuis.java",
            List.of(
                "BCSiliconGuis.MENU_AD_CRAFTING_TABLE",
                "BCSiliconGuis.MENU_ASSEMBLY_TABLE",
                "BCSiliconGuis.MENU_CHARGING_TABLE",
                "BCSiliconGuis.MENU_GATE",
                "BCSiliconGuis.MENU_INTEGRATION_TABLE",
                "BCSiliconGuis.MENU_PROGRAMMING_TABLE"
            )
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/transport/BCTransportGuis.java",
            "src/main/java/buildcraft/transport/BCTransportClientGuis.java",
            List.of(
                "BCTransportGuis.MENU_PIPE_DIAMOND_WOOD",
                "BCTransportGuis.MENU_PIPE_DIAMOND",
                "BCTransportGuis.MENU_FILTERED_BUFFER",
                "BCTransportGuis.MENU_PIPE_EMZULI"
            )
        ),
        new ModuleMenus(
            "src/main/java/buildcraft/robotics/BCRoboticsGuis.java",
            "src/main/java/buildcraft/robotics/BCRoboticsClientGuis.java",
            List.of(
                "BCRoboticsGuis.MENU_ZONE_PLANNER",
                "BCRoboticsGuis.MENU_REQUESTER"
            )
        )
    );

    @Test
    void everyRestoredMenuHasAClientScreenRegistration() throws IOException {
        int registrations = 0;
        for (ModuleMenus module : MODULE_MENUS) {
            String serverSource = Files.readString(Path.of(module.serverSource()));
            String clientSource = Files.readString(Path.of(module.clientSource()));
            for (String menuExpression : module.menuExpressions()) {
                String field = menuExpression.substring(menuExpression.lastIndexOf('.') + 1);
                assertTrue(serverSource.contains(field), () -> "Missing menu field " + menuExpression);
                assertTrue(
                    clientSource.contains("MenuScreens.register(" + menuExpression + ".get()"),
                    () -> "Missing client screen registration for " + menuExpression
                );
                registrations++;
            }
        }
        assertEquals(28, registrations, "The complete restored menu/screen inventory changed unexpectedly");
    }

    @Test
    void everyDirectBuildCraftGuiResourceReferenceResolves() throws IOException {
        Set<String> references = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(JAVA_ROOT)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String java = Files.readString(source);
                collectAndVerify(java, source, SINGLE_ARGUMENT_RESOURCE, references);
                collectAndVerify(java, source, TWO_ARGUMENT_RESOURCE, references);
            }
        }
        assertTrue(references.size() >= 40, "Too few GUI resource references were audited: " + references.size());
    }

    @Test
    void guideItemsReachTheFunctionalScreenAndPageContentIsBounded() throws IOException {
        String guideItem = Files.readString(
            Path.of("src/main/java/buildcraft/lib/item/ItemGuide.java")
        );
        String noteItem = Files.readString(
            Path.of("src/main/java/buildcraft/lib/item/ItemGuideNote.java")
        );
        String bridge = Files.readString(
            Path.of("src/main/java/buildcraft/lib/guide/GuideClientBridge.java")
        );
        String hooks = Files.readString(
            Path.of("src/main/java/buildcraft/lib/client/guide/GuideClientHooks.java")
        );
        String screen = Files.readString(
            Path.of("src/main/java/buildcraft/lib/client/guide/GuideScreen.java")
        );

        assertTrue(guideItem.contains("GuideClientBridge.openGuide"));
        assertTrue(noteItem.contains("GuideClientBridge.openNote"));
        assertTrue(bridge.contains("GuideClientHooks.openGuide"));
        assertTrue(bridge.contains("GuideClientHooks.openNote"));
        assertTrue(hooks.contains("minecraft.setScreen(GuideScreen.guide"));
        assertTrue(hooks.contains("minecraft.setScreen(GuideScreen.note"));
        assertTrue(screen.contains("font.split(component, textWidth)"));
        assertTrue(screen.contains("if (page.size() >= lineLimit)"));
        assertTrue(screen.contains("font.plainSubstrByWidth(label, PAGE_TEXT_WIDTH - 4)"));
    }

    private static void collectAndVerify(
        String java,
        Path source,
        Pattern pattern,
        Set<String> references
    ) {
        Matcher matcher = pattern.matcher(java);
        while (matcher.find()) {
            String namespace = matcher.group(1);
            String resourcePath = matcher.group(2);
            String id = namespace + ":" + resourcePath;
            Path expected = ASSET_ROOT.resolve(namespace).resolve(resourcePath);
            assertTrue(
                Files.isRegularFile(expected),
                () -> source + " references missing GUI resource " + id + " (expected " + expected + ")"
            );
            references.add(id);
        }
    }

    private record ModuleMenus(String serverSource, String clientSource, List<String> menuExpressions) {
    }
}
