/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Parsed, searchable documentation loaded from the bundled BuildCraftGuide pack. */
public record GuideDocument(
    String id,
    String namespace,
    String category,
    String path,
    String title,
    List<GuideLine> lines,
    String searchText
) {
    public GuideDocument {
        id = Objects.requireNonNull(id, "id");
        namespace = Objects.requireNonNull(namespace, "namespace");
        category = Objects.requireNonNull(category, "category");
        path = Objects.requireNonNull(path, "path");
        title = Objects.requireNonNull(title, "title");
        lines = List.copyOf(lines);
        searchText = Objects.requireNonNull(searchText, "searchText").toLowerCase(Locale.ROOT);
    }
}
