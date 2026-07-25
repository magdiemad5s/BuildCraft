/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import java.util.Objects;

/**
 * A loader-neutral line from a BuildCraft guide document.
 *
 * <p>The parser deliberately keeps raw prose as plain text. The client turns it
 * into literal components, which is important because historical documentation
 * contains literal percent signs that must never be interpreted as translation
 * format arguments.</p>
 */
public record GuideLine(Style style, String text, String target) {
    public GuideLine {
        style = Objects.requireNonNull(style, "style");
        text = Objects.requireNonNullElse(text, "");
        target = Objects.requireNonNullElse(target, "");
    }

    public GuideLine(Style style, String text) {
        this(style, text, "");
    }

    public enum Style {
        NORMAL,
        HEADING,
        SUBHEADING,
        NOTE,
        CODE,
        LINK,
        RECIPE,
        IMAGE,
        BLANK,
        PAGE_BREAK
    }
}
