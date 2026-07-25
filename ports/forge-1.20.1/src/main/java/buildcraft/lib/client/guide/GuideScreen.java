/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.client.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import buildcraft.lib.BCLibConfig;
import buildcraft.lib.guide.GuideContracts;
import buildcraft.lib.guide.GuideDocument;
import buildcraft.lib.guide.GuideIndex;
import buildcraft.lib.guide.GuideLine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Searchable, resource-backed BuildCraft guide and guide-note reader.
 *
 * <p>This intentionally uses the original page, note, and navigation artwork.
 * Documentation prose is always rendered through literal components so that
 * strings such as {@code 25%} cannot be consumed as translation formatting.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class GuideScreen extends Screen {
    private static final ResourceLocation LEFT_PAGE =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/left_page.png");
    private static final ResourceLocation LEFT_PAGE_FIRST =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/left_page_first.png");
    private static final ResourceLocation RIGHT_PAGE =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/right_page.png");
    private static final ResourceLocation RIGHT_PAGE_LAST =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/right_page_last.png");
    private static final ResourceLocation NOTE =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/note.png");
    private static final ResourceLocation ICONS =
        new ResourceLocation("buildcraftlib", "textures/gui/guide/icons.png");

    private static final int PAGE_TEXTURE_WIDTH = 193;
    private static final int PAGE_TEXTURE_HEIGHT = 248;
    private static final int PAGE_TEXT_WIDTH = 168;
    private static final int PAGE_TEXT_HEIGHT = 190;
    private static final int PAGE_LINES = 18;
    private static final int NOTE_WIDTH = 131;
    private static final int NOTE_HEIGHT = 164;
    private static final int NOTE_TEXT_WIDTH = 105;
    private static final int NOTE_LINES = 12;
    private static final int LINE_HEIGHT = 10;

    private final GuideIndex index;
    private final String bookName;
    private final boolean noteMode;
    private final Deque<GuideDocument> history = new ArrayDeque<>();

    private GuideDocument document;
    private EditBox search;
    private List<GuideDocument> results = List.of();
    private List<List<RenderedLine>> documentPages = List.of();
    private final List<HitArea> hitAreas = new ArrayList<>();
    private int currentPage;
    private int bookLeft;
    private int bookTop;
    private boolean twoPage;

    private GuideScreen(
        GuideIndex index,
        String bookName,
        GuideDocument document,
        boolean noteMode
    ) {
        super(
            noteMode
                ? Component.translatable("item.buildcraftlib.guide_note")
                : Component.translatable("item.buildcraftlib.guide")
        );
        this.index = index;
        this.bookName = bookName;
        this.document = document;
        this.noteMode = noteMode;
    }

    public static GuideScreen guide(GuideIndex index, String bookName) {
        String selectedBook = bookName == null || bookName.isBlank()
            ? GuideContracts.DEFAULT_BOOK
            : bookName;
        return new GuideScreen(index, selectedBook, null, false);
    }

    public static GuideScreen note(GuideIndex index, GuideDocument document) {
        return new GuideScreen(index, GuideContracts.DEFAULT_BOOK, document, true);
    }

    @Override
    protected void init() {
        calculateGeometry();
        if (!noteMode) {
            search = new EditBox(
                font,
                bookLeft + 25,
                bookTop + 27,
                PAGE_TEXT_WIDTH - 4,
                14,
                Component.translatable("buildcraft.guide.search")
            );
            search.setHint(Component.translatable("buildcraft.guide.search"));
            search.setMaxLength(80);
            search.setResponder(ignored -> {
                currentPage = 0;
                refreshResults();
            });
            addRenderableWidget(search);
            search.visible = document == null;
        }
        if (document == null) {
            refreshResults();
        } else {
            layoutDocument();
        }
    }

    private void calculateGeometry() {
        if (noteMode) {
            twoPage = false;
            bookLeft = (width - NOTE_WIDTH) / 2;
            bookTop = (height - NOTE_HEIGHT) / 2;
        } else {
            twoPage = width >= PAGE_TEXTURE_WIDTH * 2 + 24;
            int bookWidth = twoPage ? PAGE_TEXTURE_WIDTH * 2 : PAGE_TEXTURE_WIDTH;
            bookLeft = (width - bookWidth) / 2;
            bookTop = Math.max(4, (height - PAGE_TEXTURE_HEIGHT) / 2);
        }
    }

    private void refreshResults() {
        String query = search == null ? "" : search.getValue();
        results = index.search(bookName, query, BCLibConfig.maxGuideSearchCount);
        int maxPage = Math.max(0, resultPageCount() - 1);
        currentPage = Mth.clamp(currentPage, 0, maxPage);
    }

    private void layoutDocument() {
        int lineLimit = noteMode ? NOTE_LINES : PAGE_LINES;
        int textWidth = noteMode ? NOTE_TEXT_WIDTH : PAGE_TEXT_WIDTH;
        List<List<RenderedLine>> pages = new ArrayList<>();
        List<RenderedLine> page = new ArrayList<>(lineLimit);
        pages.add(page);

        for (GuideLine sourceLine : document.lines()) {
            if (sourceLine.style() == GuideLine.Style.PAGE_BREAK) {
                if (!page.isEmpty()) {
                    page = new ArrayList<>(lineLimit);
                    pages.add(page);
                }
                continue;
            }
            if (sourceLine.style() == GuideLine.Style.BLANK) {
                if (!page.isEmpty() && page.size() < lineLimit) {
                    page.add(RenderedLine.blank());
                }
                continue;
            }

            Component component = componentFor(sourceLine);
            List<FormattedCharSequence> wrapped = font.split(component, textWidth);
            if (wrapped.isEmpty()) {
                wrapped = List.of(FormattedCharSequence.EMPTY);
            }
            for (FormattedCharSequence sequence : wrapped) {
                if (page.size() >= lineLimit) {
                    page = new ArrayList<>(lineLimit);
                    pages.add(page);
                }
                page.add(new RenderedLine(sequence, sourceLine.target(), sourceLine.style()));
            }
        }

        if (pages.size() > 1 && pages.get(pages.size() - 1).isEmpty()) {
            pages.remove(pages.size() - 1);
        }
        documentPages = List.copyOf(pages);
        currentPage = Mth.clamp(currentPage, 0, Math.max(0, documentPages.size() - 1));
    }

    private Component componentFor(GuideLine line) {
        MutableComponent component;
        if ((line.style() == GuideLine.Style.HEADING || line.style() == GuideLine.Style.SUBHEADING)
            && isTranslationKey(line.text())) {
            component = Component.translatable(line.text());
        } else {
            // Never pass documentation prose to Component.translatable: literal '%' is valid guide text.
            component = Component.literal(line.text());
        }
        return switch (line.style()) {
            case HEADING -> component.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
            case SUBHEADING -> component.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD);
            case NOTE -> component.withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
            case CODE -> component.withStyle(ChatFormatting.DARK_GRAY);
            case LINK -> component.withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.UNDERLINE);
            case RECIPE -> component.withStyle(ChatFormatting.DARK_GREEN);
            case IMAGE -> component.withStyle(ChatFormatting.DARK_AQUA);
            default -> component;
        };
    }

    private static boolean isTranslationKey(String text) {
        return text.indexOf(' ') < 0 && text.indexOf('.') > 0 && I18n.exists(text);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        hitAreas.clear();
        if (noteMode) {
            renderNote(graphics, mouseX, mouseY);
        } else {
            renderBook(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBook(GuiGraphics graphics, int mouseX, int mouseY) {
        ResourceLocation leftTexture = currentPage == 0 ? LEFT_PAGE_FIRST : LEFT_PAGE;
        graphics.blit(
            leftTexture,
            bookLeft,
            bookTop,
            0,
            0,
            PAGE_TEXTURE_WIDTH,
            PAGE_TEXTURE_HEIGHT,
            256,
            256
        );
        if (twoPage) {
            boolean lastSpread = !canGoNext();
            graphics.blit(
                lastSpread ? RIGHT_PAGE_LAST : RIGHT_PAGE,
                bookLeft + PAGE_TEXTURE_WIDTH,
                bookTop,
                0,
                0,
                PAGE_TEXTURE_WIDTH,
                PAGE_TEXTURE_HEIGHT,
                256,
                256
            );
        }

        if (document == null) {
            renderContents(graphics);
        } else {
            renderDocumentTitle(graphics, bookLeft + 23, bookTop + 25, PAGE_TEXT_WIDTH);
            renderDocumentPage(graphics, currentPage, bookLeft + 23, bookTop + 43);
            if (twoPage && currentPage + 1 < documentPages.size()) {
                renderDocumentTitle(
                    graphics,
                    bookLeft + PAGE_TEXTURE_WIDTH + 4,
                    bookTop + 25,
                    PAGE_TEXT_WIDTH
                );
                renderDocumentPage(
                    graphics,
                    currentPage + 1,
                    bookLeft + PAGE_TEXTURE_WIDTH + 4,
                    bookTop + 43
                );
            }
        }
        renderNavigation(graphics, mouseX, mouseY);
    }

    private void renderContents(GuiGraphics graphics) {
        String heading = GuideContracts.META_BOOK.equals(bookName)
            ? I18n.get("buildcraft.guide.book.meta.name")
            : I18n.get("buildcraft.guide.chapter.contents");
        graphics.drawCenteredString(
            font,
            Component.literal(heading),
            bookLeft + 23 + PAGE_TEXT_WIDTH / 2,
            bookTop + 45,
            0x5f4934
        );

        if (index.isEmpty()) {
            graphics.drawWordWrap(
                font,
                Component.translatable("buildcraft.guide.empty"),
                bookLeft + 23,
                bookTop + 68,
                PAGE_TEXT_WIDTH,
                0x5f2020
            );
            return;
        }

        int perPhysicalPage = 15;
        int columns = twoPage ? 2 : 1;
        int first = currentPage * perPhysicalPage * columns;
        for (int column = 0; column < columns; column++) {
            int x = column == 0 ? bookLeft + 23 : bookLeft + PAGE_TEXTURE_WIDTH + 4;
            int start = first + column * perPhysicalPage;
            if (column == 1) {
                graphics.drawCenteredString(
                    font,
                    Component.literal(heading),
                    x + PAGE_TEXT_WIDTH / 2,
                    bookTop + 27,
                    0x5f4934
                );
            }
            for (int row = 0; row < perPhysicalPage; row++) {
                int resultIndex = start + row;
                if (resultIndex >= results.size()) {
                    break;
                }
                GuideDocument result = results.get(resultIndex);
                int y = bookTop + 61 + row * LINE_HEIGHT;
                String label = result.title() + " \u00b7 " + moduleName(result.namespace());
                label = font.plainSubstrByWidth(label, PAGE_TEXT_WIDTH - 4);
                graphics.drawString(font, Component.literal(label), x + 2, y, 0x342b22, false);
                hitAreas.add(new HitArea(x, y - 1, PAGE_TEXT_WIDTH, LINE_HEIGHT, result, ""));
            }
        }
    }

    private void renderDocumentTitle(GuiGraphics graphics, int x, int y, int textWidth) {
        String titleText = font.plainSubstrByWidth(document.title(), textWidth - 4);
        graphics.drawCenteredString(
            font,
            Component.literal(titleText).withStyle(ChatFormatting.BOLD),
            x + textWidth / 2,
            y,
            0x5f4934
        );
    }

    private void renderDocumentPage(GuiGraphics graphics, int pageIndex, int x, int y) {
        if (pageIndex < 0 || pageIndex >= documentPages.size()) {
            return;
        }
        List<RenderedLine> page = documentPages.get(pageIndex);
        for (int row = 0; row < page.size(); row++) {
            RenderedLine line = page.get(row);
            int lineY = y + row * LINE_HEIGHT;
            graphics.drawString(font, line.text(), x, lineY, 0x342b22, false);
            if (!line.target().isBlank()) {
                hitAreas.add(
                    new HitArea(
                        x,
                        lineY - 1,
                        noteMode ? NOTE_TEXT_WIDTH : PAGE_TEXT_WIDTH,
                        LINE_HEIGHT,
                        null,
                        line.target()
                    )
                );
            }
        }
    }

    private void renderNote(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(NOTE, bookLeft, bookTop, 0, 0, NOTE_WIDTH, NOTE_HEIGHT, 256, 256);
        renderDocumentTitle(graphics, bookLeft + 13, bookTop + 14, NOTE_TEXT_WIDTH);
        renderDocumentPage(graphics, currentPage, bookLeft + 13, bookTop + 31);
        renderNavigation(graphics, mouseX, mouseY);
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY) {
        int bottom = noteMode ? bookTop + 145 : bookTop + 226;
        int width = noteMode ? NOTE_WIDTH : (twoPage ? PAGE_TEXTURE_WIDTH * 2 : PAGE_TEXTURE_WIDTH);
        if (canGoPrevious()) {
            boolean hovered = inside(mouseX, mouseY, bookLeft + 8, bottom, 18, 10);
            graphics.blit(ICONS, bookLeft + 8, bottom, 23, hovered ? 152 : 139, 18, 10, 256, 256);
        }
        if (canGoNext()) {
            int x = bookLeft + width - 26;
            boolean hovered = inside(mouseX, mouseY, x, bottom, 18, 10);
            graphics.blit(ICONS, x, bottom, 0, hovered ? 152 : 139, 18, 10, 256, 256);
        }
        if (!noteMode && document != null) {
            int x = bookLeft + width / 2 - 8;
            boolean hovered = inside(mouseX, mouseY, x, bottom, 17, 9);
            graphics.blit(ICONS, x, bottom, 48, hovered ? 152 : 139, 17, 9, 256, 256);
        }

        int pageCount = document == null ? resultPageCount() : documentPages.size();
        if (pageCount > 1) {
            int displayPage = document == null
                ? currentPage + 1
                : Math.min(currentPage + (twoPage ? 2 : 1), pageCount);
            String pageLabel = displayPage + " / " + pageCount;
            graphics.drawCenteredString(
                font,
                Component.literal(pageLabel),
                bookLeft + width / 2,
                bottom - 1,
                0x806d55
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (HitArea area : hitAreas) {
                if (!area.contains(mouseX, mouseY)) {
                    continue;
                }
                if (area.document() != null) {
                    openDocument(area.document(), false);
                    return true;
                }
                GuideDocument linked = index.lookup(area.target());
                if (linked != null) {
                    openDocument(linked, true);
                    return true;
                }
            }

            int bottom = noteMode ? bookTop + 145 : bookTop + 226;
            int bookWidth = noteMode ? NOTE_WIDTH : (twoPage ? PAGE_TEXTURE_WIDTH * 2 : PAGE_TEXTURE_WIDTH);
            if (canGoPrevious() && inside(mouseX, mouseY, bookLeft + 8, bottom, 18, 10)) {
                previousPage();
                return true;
            }
            if (canGoNext() && inside(mouseX, mouseY, bookLeft + bookWidth - 26, bottom, 18, 10)) {
                nextPage();
                return true;
            }
            if (!noteMode && document != null
                && inside(mouseX, mouseY, bookLeft + bookWidth / 2 - 8, bottom, 17, 9)) {
                goBack();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && canGoPrevious()) {
            previousPage();
            return true;
        }
        if (delta < 0 && canGoNext()) {
            nextPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean searchFocused = search != null && search.isFocused();
        if (!searchFocused && keyCode == GLFW.GLFW_KEY_LEFT && canGoPrevious()) {
            previousPage();
            return true;
        }
        if (!searchFocused && keyCode == GLFW.GLFW_KEY_RIGHT && canGoNext()) {
            nextPage();
            return true;
        }
        if (!noteMode && document != null && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openDocument(GuideDocument selected, boolean rememberCurrent) {
        if (rememberCurrent && document != null) {
            history.push(document);
        }
        document = selected;
        currentPage = 0;
        if (search != null) {
            search.visible = false;
            search.setFocused(false);
        }
        layoutDocument();
    }

    private void goBack() {
        if (!history.isEmpty()) {
            document = history.pop();
            currentPage = 0;
            layoutDocument();
            return;
        }
        document = null;
        currentPage = 0;
        if (search != null) {
            search.visible = true;
            search.setFocused(true);
        }
        refreshResults();
    }

    private boolean canGoPrevious() {
        return currentPage > 0;
    }

    private boolean canGoNext() {
        int step = pageStep();
        if (document == null) {
            return currentPage + 1 < resultPageCount();
        }
        return currentPage + step < documentPages.size();
    }

    private void previousPage() {
        currentPage = Math.max(0, currentPage - (document == null ? 1 : pageStep()));
    }

    private void nextPage() {
        currentPage += document == null ? 1 : pageStep();
    }

    private int pageStep() {
        return !noteMode && twoPage ? 2 : 1;
    }

    private int resultPageCount() {
        int perScreen = 15 * (twoPage ? 2 : 1);
        return Math.max(1, (results.size() + perScreen - 1) / perScreen);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String moduleName(String namespace) {
        String value = namespace.toLowerCase(Locale.ROOT);
        if (value.startsWith("buildcraft")) {
            value = value.substring("buildcraft".length());
        }
        return value.isBlank() ? "BuildCraft" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record RenderedLine(
        FormattedCharSequence text,
        String target,
        GuideLine.Style style
    ) {
        private static RenderedLine blank() {
            return new RenderedLine(FormattedCharSequence.EMPTY, "", GuideLine.Style.BLANK);
        }
    }

    private record HitArea(
        int x,
        int y,
        int width,
        int height,
        GuideDocument document,
        String target
    ) {
        private boolean contains(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, width, height);
        }
    }
}
