/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.client.guide;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import buildcraft.api.core.BCLog;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideDocument;
import buildcraft.lib.guide.GuideDocumentParser;
import buildcraft.lib.guide.GuideIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client resource-reload listener for the bundled BuildCraftGuide documents.
 *
 * <p>Resource packs can replace or add pages using the original
 * {@code assets/<namespace>/compat/buildcraft/guide/<locale>/...} layout.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class GuideRepository extends SimplePreparableReloadListener<GuideIndex> {
    public static final GuideRepository INSTANCE = new GuideRepository();

    private volatile GuideIndex index = GuideIndex.empty();

    private GuideRepository() {
    }

    @Override
    protected GuideIndex prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        return load(resourceManager, locale, BCLibConfig.guideShowDetail);
    }

    @Override
    protected void apply(GuideIndex prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        index = prepared;
        BCLog.logger.info("[lib.guide] Loaded {} guide documents", prepared.size());
    }

    public GuideIndex current() {
        return index;
    }

    /** Startup fallback for clients that open the item before the first reload listener pass finishes. */
    public GuideIndex loadNow(ResourceManager resourceManager) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        GuideIndex loaded = load(resourceManager, locale, BCLibConfig.guideShowDetail);
        index = loaded;
        return loaded;
    }

    static GuideIndex load(ResourceManager resourceManager, String requestedLocale, boolean showDetail) {
        String selectedLocale = normalizeLocale(requestedLocale);
        String root = GuideContracts.DOCUMENT_ROOT + "/";
        Map<String, LocalizedResource> selected = new HashMap<>();

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
            GuideContracts.DOCUMENT_ROOT,
            id -> id.getPath().endsWith(".md")
        );
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            String path = location.getPath();
            if (!path.startsWith(root)) {
                continue;
            }
            String relative = path.substring(root.length());
            int localeSlash = relative.indexOf('/');
            if (localeSlash <= 0 || localeSlash + 1 >= relative.length()) {
                continue;
            }
            String locale = normalizeLocale(relative.substring(0, localeSlash));
            int priority = locale.equals(selectedLocale) ? 2 : locale.equals("en_us") ? 1 : 0;
            if (priority == 0) {
                continue;
            }
            String documentPath = relative.substring(localeSlash + 1, relative.length() - ".md".length());
            String id = location.getNamespace() + ":" + documentPath;
            LocalizedResource previous = selected.get(id);
            if (previous == null || priority > previous.priority()) {
                selected.put(id, new LocalizedResource(entry.getValue(), priority));
            }
        }

        List<String> ids = new ArrayList<>(selected.keySet());
        ids.sort(String::compareTo);
        List<GuideDocument> documents = new ArrayList<>(ids.size());
        for (String id : ids) {
            try (BufferedReader reader = selected.get(id).resource().openAsReader()) {
                StringBuilder source = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    source.append(line).append('\n');
                }
                documents.add(GuideDocumentParser.parse(id, source.toString(), showDetail));
            } catch (IOException | RuntimeException exception) {
                BCLog.logger.error("[lib.guide] Failed to load guide document {}", id, exception);
            }
        }
        return new GuideIndex(documents);
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en_us";
        }
        return locale.replace('-', '_').toLowerCase(Locale.ROOT);
    }

    private record LocalizedResource(Resource resource, int priority) {
    }
}
