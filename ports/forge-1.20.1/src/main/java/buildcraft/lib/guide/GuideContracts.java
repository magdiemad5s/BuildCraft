/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

/**
 * Compatibility-sensitive names used by the BuildCraft guide items.
 *
 * <p>These values intentionally retain the 1.12.2 spellings. Existing item
 * stacks use {@code BookName} and {@code note_id}, so changing either key would
 * silently detach old guide books and notes from their content.</p>
 */
public final class GuideContracts {
    public static final String GUIDE_ITEM_PATH = "guide";
    public static final String GUIDE_NOTE_ITEM_PATH = "guide_note";
    public static final String GUIDE_ITEM_ID = "buildcraftlib:" + GUIDE_ITEM_PATH;
    public static final String GUIDE_NOTE_ITEM_ID = "buildcraftlib:" + GUIDE_NOTE_ITEM_PATH;
    public static final String DEFAULT_BOOK = "buildcraftcore:main";
    public static final String META_BOOK = "buildcraftlib:meta";
    public static final String TAG_BOOK_NAME = "BookName";
    public static final String TAG_NOTE_ID = "note_id";

    /** Root used by the pinned BuildCraftGuide resource pack. */
    public static final String DOCUMENT_ROOT = "compat/buildcraft/guide";

    private GuideContracts() {
    }
}
