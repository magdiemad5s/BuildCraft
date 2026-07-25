/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.core.list;

/** Bounds and text policy shared by the List menu's client and server paths. */
public final class ListMenuStatePolicy {
    public static final int BUTTON_COUNT = 3;
    public static final int MAX_LABEL_LENGTH = 32;

    private ListMenuStatePolicy() {
    }

    public static boolean isValidLine(int lineIndex, int lineCount) {
        return lineIndex >= 0 && lineIndex < lineCount;
    }

    public static boolean isValidSlot(int lineIndex, int slotIndex, int lineCount, int slotCount) {
        return isValidLine(lineIndex, lineCount) && slotIndex >= 0 && slotIndex < slotCount;
    }

    public static boolean isValidButton(int lineIndex, int buttonIndex, int lineCount) {
        return isValidLine(lineIndex, lineCount)
            && buttonIndex >= 0
            && buttonIndex < BUTTON_COUNT;
    }

    public static String sanitizeLabel(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder clean = new StringBuilder(Math.min(value.length(), MAX_LABEL_LENGTH));
        for (int index = 0; index < value.length() && clean.length() < MAX_LABEL_LENGTH; index++) {
            char character = value.charAt(index);
            if (character >= ' ' && character != 0x7F) {
                clean.append(character);
            }
        }
        return clean.toString();
    }
}
