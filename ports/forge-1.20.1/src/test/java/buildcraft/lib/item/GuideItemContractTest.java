package buildcraft.lib.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideNbtCodec;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class GuideItemContractTest {
    @Test
    void preservesTheExactLegacyRegistryIds() {
        assertEquals("guide", GuideContracts.GUIDE_ITEM_PATH);
        assertEquals("guide_note", GuideContracts.GUIDE_NOTE_ITEM_PATH);
        assertEquals("buildcraftlib:guide", GuideContracts.GUIDE_ITEM_ID);
        assertEquals("buildcraftlib:guide_note", GuideContracts.GUIDE_NOTE_ITEM_ID);
    }

    @Test
    void guideBookRoundTripsTheExactLegacyBookNameKey() {
        CompoundTag tag = new CompoundTag();

        assertEquals("BookName", GuideContracts.TAG_BOOK_NAME);
        assertEquals(GuideContracts.DEFAULT_BOOK, GuideNbtCodec.getBookName(null));
        assertEquals(GuideContracts.DEFAULT_BOOK, GuideNbtCodec.getBookName(tag));

        GuideNbtCodec.setBookName(tag, GuideContracts.META_BOOK);
        assertEquals(GuideContracts.META_BOOK, GuideNbtCodec.getBookName(tag));
        assertEquals(GuideContracts.META_BOOK, tag.getString("BookName"));
        assertFalse(tag.contains("bookName"));
        assertFalse(tag.contains("book_name"));

        GuideNbtCodec.setBookName(tag, GuideContracts.DEFAULT_BOOK);
        assertFalse(tag.contains("BookName"));
        assertEquals(GuideContracts.DEFAULT_BOOK, GuideNbtCodec.getBookName(tag));
    }

    @Test
    void guideNoteRoundTripsTheExactLegacyNoteIdKey() {
        CompoundTag tag = new CompoundTag();

        assertEquals("note_id", GuideContracts.TAG_NOTE_ID);
        assertEquals("", GuideNbtCodec.getNoteId(null));
        assertEquals("", GuideNbtCodec.getNoteId(tag));

        GuideNbtCodec.setNoteId(tag, "buildcraftfactory:block/tank");
        assertEquals(
            "buildcraftfactory:block/tank",
            GuideNbtCodec.getNoteId(tag)
        );
        assertEquals(
            "buildcraftfactory:block/tank",
            tag.getString("note_id")
        );
        assertFalse(tag.contains("noteId"));

        GuideNbtCodec.setNoteId(tag, " ");
        assertFalse(tag.contains("note_id"));
        assertEquals("", GuideNbtCodec.getNoteId(tag));
    }
}
