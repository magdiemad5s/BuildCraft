/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Bootstrap-free codec for the compatibility-sensitive data on guide items. */
public final class GuideNbtCodec {
    private GuideNbtCodec() {
    }

    public static String getBookName(CompoundTag tag) {
        if (tag == null || !tag.contains(GuideContracts.TAG_BOOK_NAME, Tag.TAG_STRING)) {
            return GuideContracts.DEFAULT_BOOK;
        }
        String value = tag.getString(GuideContracts.TAG_BOOK_NAME);
        return value.isBlank() ? GuideContracts.DEFAULT_BOOK : value;
    }

    public static void setBookName(CompoundTag tag, String book) {
        if (book == null || book.isBlank() || GuideContracts.DEFAULT_BOOK.equals(book)) {
            if (tag != null) {
                tag.remove(GuideContracts.TAG_BOOK_NAME);
            }
        } else {
            if (tag == null) {
                throw new IllegalArgumentException("A tag is required for a custom guide book");
            }
            tag.putString(GuideContracts.TAG_BOOK_NAME, book);
        }
    }

    public static String getNoteId(CompoundTag tag) {
        return tag == null ? "" : tag.getString(GuideContracts.TAG_NOTE_ID);
    }

    public static void setNoteId(CompoundTag tag, String noteId) {
        if (noteId == null || noteId.isBlank()) {
            if (tag != null) {
                tag.remove(GuideContracts.TAG_NOTE_ID);
            }
        } else {
            if (tag == null) {
                throw new IllegalArgumentException("A tag is required for a populated guide note");
            }
            tag.putString(GuideContracts.TAG_NOTE_ID, noteId);
        }
    }
}
