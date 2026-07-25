/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable document index used by both the guide contents and guide notes. */
public final class GuideIndex {
    private static final GuideIndex EMPTY = new GuideIndex(List.of());

    private final List<GuideDocument> documents;
    private final Map<String, GuideDocument> byId;

    public GuideIndex(List<GuideDocument> documents) {
        List<GuideDocument> sorted = new ArrayList<>(documents);
        sorted.sort(
            Comparator.comparing(GuideDocument::namespace)
                .thenComparing(GuideDocument::category)
                .thenComparing(GuideDocument::title, String.CASE_INSENSITIVE_ORDER)
        );
        this.documents = List.copyOf(sorted);
        Map<String, GuideDocument> indexed = new HashMap<>();
        for (GuideDocument document : sorted) {
            indexed.put(document.id().toLowerCase(Locale.ROOT), document);
        }
        this.byId = Map.copyOf(indexed);
    }

    public static GuideIndex empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return documents.isEmpty();
    }

    public int size() {
        return documents.size();
    }

    public List<GuideDocument> documents() {
        return documents;
    }

    public List<GuideDocument> search(String bookName, String query, int limit) {
        int boundedLimit = Math.max(1, limit);
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        boolean metaOnly = GuideContracts.META_BOOK.equalsIgnoreCase(bookName);
        List<GuideDocument> result = new ArrayList<>();
        for (GuideDocument document : documents) {
            if (metaOnly && !"config".equals(document.category())) {
                continue;
            }
            if (!needle.isEmpty() && !document.searchText().contains(needle)) {
                continue;
            }
            result.add(document);
            if (result.size() >= boundedLimit) {
                break;
            }
        }
        return List.copyOf(result);
    }

    /**
     * Resolves canonical page IDs, resource-pack paths, note fragments, and
     * item/pipe IDs used by legacy link tags.
     */
    public GuideDocument lookup(String rawId) {
        String id = normalize(rawId);
        if (id.isEmpty()) {
            return null;
        }
        GuideDocument direct = byId.get(id);
        if (direct != null) {
            return direct;
        }

        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        if (path.startsWith("pipe_")) {
            String pipePath = path.substring("pipe_".length());
            direct = byId.get(namespace + ":pipe/" + pipePath);
            if (direct != null) {
                return direct;
            }
        }

        for (String category : List.of("item", "block", "pipe", "action", "trigger", "config")) {
            direct = byId.get(namespace + ":" + category + "/" + path);
            if (direct != null) {
                return direct;
            }
        }

        GuideDocument unique = null;
        String suffix = "/" + path;
        for (GuideDocument document : documents) {
            if ((!namespace.isEmpty() && !namespace.equals(document.namespace()))
                || !document.id().endsWith(suffix)) {
                continue;
            }
            if (unique != null) {
                return null;
            }
            unique = document;
        }
        return unique;
    }

    static String normalize(String rawId) {
        if (rawId == null) {
            return "";
        }
        String value = rawId.strip().replace('\\', '/').toLowerCase(Locale.ROOT);
        int fragment = value.indexOf('#');
        if (fragment >= 0) {
            value = value.substring(0, fragment);
        }
        String resourcePrefix = GuideContracts.DOCUMENT_ROOT + "/";
        int root = value.indexOf(resourcePrefix);
        if (root >= 0) {
            int colon = value.indexOf(':');
            String namespace = colon >= 0 && colon < root ? value.substring(0, colon) : "";
            String relative = value.substring(root + resourcePrefix.length());
            int localeSlash = relative.indexOf('/');
            if (localeSlash >= 0) {
                relative = relative.substring(localeSlash + 1);
            }
            if (relative.endsWith(".md")) {
                relative = relative.substring(0, relative.length() - 3);
            }
            value = namespace.isEmpty() ? relative : namespace + ":" + relative;
        } else if (value.endsWith(".md")) {
            value = value.substring(0, value.length() - 3);
        }
        return value;
    }
}
