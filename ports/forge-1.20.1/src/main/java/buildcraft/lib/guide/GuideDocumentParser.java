/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the Markdown-plus-BuildCraft markup used by BuildCraftGuide into a
 * small rendering model suitable for the modern client screen.
 */
public final class GuideDocumentParser {
    private static final Pattern CHAPTER =
        Pattern.compile("(?i)<chapter\\b([^>]*)/\\s*>");
    private static final Pattern NEW_PAGE =
        Pattern.compile("(?i)<new_page\\s*/\\s*>");
    private static final Pattern LINK =
        Pattern.compile("(?i)<link\\b([^>]*)/\\s*>");
    private static final Pattern RECIPE =
        Pattern.compile("(?i)<(recipes_usages|recipes|usages|recipe)\\b([^>]*)/\\s*>");
    private static final Pattern IMAGE =
        Pattern.compile("(?i)<image\\b([^>]*)/\\s*>");
    private static final Pattern ATTRIBUTE =
        Pattern.compile("([a-zA-Z0-9_]+)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern MARKDOWN_LINK =
        Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern MARKDOWN_SPECIAL =
        Pattern.compile("\\$\\[special\\.([a-zA-Z0-9_]+)](?:\\(([^)]+)\\))?");
    private static final Pattern STYLE_TAG =
        Pattern.compile("(?i)</?(?:bold|italic|underline|strikethrough|black|dark_blue|dark_green|dark_aqua|dark_red|"
            + "dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)\\s*>");
    private static final Pattern REMAINING_TAG =
        Pattern.compile("</?[a-zA-Z][a-zA-Z0-9_]*(?:\\s[^>]*)?/?>");
    private static final Pattern HEADING =
        Pattern.compile("^(#{1,6})\\s*(.+)$");
    private static final Pattern DIRECTIVE =
        Pattern.compile("^@@([a-z_]+)(?::(.*))?$");

    private GuideDocumentParser() {
    }

    public static GuideDocument parse(String id, String source, boolean showDetail) {
        ParsedId parsedId = ParsedId.parse(id);
        String title = titleFor(parsedId.category(), parsedId.path());
        String text = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n');

        text = selectPairedTag(text, "lore", showDetail);
        text = selectPairedTag(text, "no_lore", !showDetail);
        text = selectPairedTag(text, "no_detail", !showDetail);

        text = replacePairedTag(text, "hint", "\n@@note:Hint\n", "\n");
        text = replacePairedTag(text, "note", "\n@@note:Note\n", "\n");
        text = replacePairedTag(text, "guide_md", "\n@@code\n", "\n@@code_end\n");
        text = replacePairedTag(text, "json_insn", "\n@@code\n", "\n@@code_end\n");

        text = replaceChapters(text);
        text = NEW_PAGE.matcher(text).replaceAll("\n@@page_break\n");
        text = replaceLinks(text);
        text = replaceRecipes(text);
        text = replaceImages(text);
        text = replaceMarkdownSpecials(text);
        text = STYLE_TAG.matcher(text).replaceAll("");
        text = decodeEntities(text);
        text = decodeEscapedProseLineBreaks(text);

        List<GuideLine> lines = parseLines(text);
        StringBuilder searchable = new StringBuilder(id).append(' ').append(title);
        for (GuideLine line : lines) {
            if (!line.text().isBlank()) {
                searchable.append(' ').append(line.text());
            }
            if (!line.target().isBlank()) {
                searchable.append(' ').append(line.target());
            }
        }
        return new GuideDocument(
            id,
            parsedId.namespace(),
            parsedId.category(),
            parsedId.path(),
            title,
            lines,
            searchable.toString()
        );
    }

    private static String selectPairedTag(String source, String tag, boolean keep) {
        Pattern pattern = Pattern.compile("(?is)<" + tag + "\\b[^>]*>(.*?)</" + tag + "\\s*>");
        Matcher matcher = pattern.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(keep ? matcher.group(1) : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replacePairedTag(String source, String tag, String before, String after) {
        Pattern pattern = Pattern.compile("(?is)<" + tag + "\\b[^>]*>(.*?)</" + tag + "\\s*>");
        Matcher matcher = pattern.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(
                result,
                Matcher.quoteReplacement(before + matcher.group(1) + after)
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceChapters(String source) {
        Matcher matcher = CHAPTER.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = attribute(matcher.group(1), "name");
            String replacement = name.isBlank() ? "\n" : "\n## " + name + "\n";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLinks(String source) {
        Matcher matcher = LINK.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String target = attribute(matcher.group(1), "to");
            matcher.appendReplacement(result, Matcher.quoteReplacement("\n@@link:" + target + "\n"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceRecipes(String source) {
        Matcher matcher = RECIPE.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String kind = matcher.group(1).toLowerCase(Locale.ROOT);
            String target = attribute(matcher.group(2), "stack");
            matcher.appendReplacement(
                result,
                Matcher.quoteReplacement("\n@@recipe:" + kind + "|" + target + "\n")
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceImages(String source) {
        Matcher matcher = IMAGE.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String target = attribute(matcher.group(1), "src");
            matcher.appendReplacement(result, Matcher.quoteReplacement("\n@@image:" + target + "\n"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceMarkdownSpecials(String source) {
        Matcher matcher = MARKDOWN_SPECIAL.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            String target = matcher.group(2) == null ? "" : matcher.group(2);
            String replacement = switch (name) {
                case "new_page" -> "\n@@page_break\n";
                case "all_crafting" -> "\n@@recipe:recipes|" + target + "\n";
                default -> "";
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static List<GuideLine> parseLines(String source) {
        List<GuideLine> lines = new ArrayList<>();
        boolean code = false;
        boolean previousBlank = true;
        for (String rawLine : source.split("\n", -1)) {
            String trimmed = rawLine.strip();
            if (trimmed.equals("@@code")) {
                code = true;
                continue;
            }
            if (trimmed.equals("@@code_end")) {
                code = false;
                continue;
            }
            if (code) {
                if (trimmed.isBlank()) {
                    if (!previousBlank && !lines.isEmpty()) {
                        lines.add(new GuideLine(GuideLine.Style.BLANK, ""));
                    }
                    previousBlank = true;
                } else {
                    lines.add(new GuideLine(GuideLine.Style.CODE, rawLine.stripTrailing()));
                    previousBlank = false;
                }
                continue;
            }

            Matcher directive = DIRECTIVE.matcher(trimmed);
            if (directive.matches()) {
                String name = directive.group(1);
                String value = directive.group(2) == null ? "" : directive.group(2);
                switch (name) {
                    case "page_break" -> lines.add(new GuideLine(GuideLine.Style.PAGE_BREAK, ""));
                    case "link" -> lines.add(new GuideLine(
                        GuideLine.Style.LINK,
                        "See also: " + displayTarget(value),
                        value
                    ));
                    case "recipe" -> lines.add(parseRecipeDirective(value));
                    case "image" -> lines.add(new GuideLine(
                        GuideLine.Style.IMAGE,
                        "Image: " + displayTarget(value),
                        value
                    ));
                    case "note" -> lines.add(new GuideLine(GuideLine.Style.NOTE, value));
                    default -> {
                        // Unknown future directives should remain visible to pack authors.
                        lines.add(new GuideLine(GuideLine.Style.CODE, trimmed));
                    }
                }
                previousBlank = false;
                continue;
            }

            Matcher heading = HEADING.matcher(trimmed);
            if (heading.matches()) {
                GuideLine.Style style = heading.group(1).length() <= 2
                    ? GuideLine.Style.HEADING
                    : GuideLine.Style.SUBHEADING;
                lines.add(new GuideLine(style, cleanInline(heading.group(2))));
                previousBlank = false;
                continue;
            }

            String cleaned = cleanInline(trimmed);
            if (cleaned.isBlank()) {
                if (!previousBlank && !lines.isEmpty()) {
                    lines.add(new GuideLine(GuideLine.Style.BLANK, ""));
                }
                previousBlank = true;
                continue;
            }

            lines.add(new GuideLine(GuideLine.Style.NORMAL, cleaned));
            previousBlank = false;
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).style() == GuideLine.Style.BLANK) {
            lines.remove(lines.size() - 1);
        }
        return List.copyOf(lines);
    }

    private static GuideLine parseRecipeDirective(String value) {
        int separator = value.indexOf('|');
        String kind = separator < 0 ? "recipes" : value.substring(0, separator);
        String target = separator < 0 ? value : value.substring(separator + 1);
        String label = switch (kind) {
            case "usages" -> "Uses";
            case "recipes_usages" -> "Recipes and uses";
            case "recipe" -> "Recipe";
            default -> "Recipes";
        };
        if (!target.isBlank()) {
            label += ": " + displayTarget(target);
        }
        return new GuideLine(GuideLine.Style.RECIPE, label, target);
    }

    private static String cleanInline(String value) {
        String cleaned = STYLE_TAG.matcher(value).replaceAll("");
        cleaned = REMAINING_TAG.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replace("**", "").replace("__", "");
        if (cleaned.startsWith("- ")) {
            cleaned = "\u2022 " + cleaned.substring(2);
        }
        Matcher links = MARKDOWN_LINK.matcher(cleaned);
        StringBuffer result = new StringBuffer();
        while (links.find()) {
            String label = links.group(1);
            String target = links.group(2);
            links.appendReplacement(result, Matcher.quoteReplacement(label + " (" + target + ")"));
        }
        links.appendTail(result);
        return result.toString().strip();
    }

    private static String attribute(String attributes, String name) {
        Matcher matcher = ATTRIBUTE.matcher(attributes == null ? "" : attributes);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(name)) {
                return matcher.group(2);
            }
        }
        return "";
    }

    private static String decodeEntities(String value) {
        return value
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&");
    }

    /**
     * Legacy guide prose sometimes stores visual line breaks as the two
     * characters {@code \n}. Decode those markers without changing examples
     * inside the guide's explicit code blocks.
     */
    private static String decodeEscapedProseLineBreaks(String source) {
        String[] physicalLines = source.split("\n", -1);
        StringBuilder decoded = new StringBuilder(source.length());
        boolean code = false;
        for (int i = 0; i < physicalLines.length; i++) {
            String line = physicalLines[i];
            String trimmed = line.strip();
            if (trimmed.equals("@@code")) {
                code = true;
            }
            decoded.append(code ? line : line.replace("\\n", "\n"));
            if (trimmed.equals("@@code_end")) {
                code = false;
            }
            if (i + 1 < physicalLines.length) {
                decoded.append('\n');
            }
        }
        return decoded.toString();
    }

    private static String displayTarget(String target) {
        int colon = target.indexOf(':');
        String path = colon >= 0 ? target.substring(colon + 1) : target;
        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        if (path.startsWith("pipe_")) {
            path = path.substring("pipe_".length());
        }
        return humanize(path);
    }

    static String titleFor(String category, String path) {
        String title = humanize(path);
        if ("pipe".equals(category) && !title.toLowerCase(Locale.ROOT).endsWith(" pipe")) {
            title += " Pipe";
        }
        return title;
    }

    static String humanize(String value) {
        String normalized = value.replace('-', '_').replace('/', '_').strip();
        StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '_') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return result.toString().strip();
    }

    private record ParsedId(String namespace, String category, String path) {
        private static ParsedId parse(String id) {
            int colon = id.indexOf(':');
            String namespace = colon < 0 ? "buildcraftlib" : id.substring(0, colon);
            String remainder = colon < 0 ? id : id.substring(colon + 1);
            int slash = remainder.indexOf('/');
            String category = slash < 0 ? "misc" : remainder.substring(0, slash);
            String path = slash < 0 ? remainder : remainder.substring(slash + 1);
            return new ParsedId(namespace, category, path);
        }
    }
}
