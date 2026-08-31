package com.hit.chatbot;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class ChatBotCommandParser {

    private static final int ZERO_WIDTH_SPACE = 0x200B;
    private static final int ZERO_WIDTH_NO_BREAK_SPACE = 0xFEFF;

    private ChatBotCommandParser() {
    }

    public static Pair<String, String> parse(String text) {
        if (text == null) {
            return Pair.of(StringUtils.EMPTY, null);
        }

        int textLength = text.length();
        int commandStart = skipSeparatorsForward(text, 0, textLength);
        if (commandStart == textLength) {
            return Pair.of(StringUtils.EMPTY, StringUtils.EMPTY);
        }
        if (text.codePointAt(commandStart) != '/') {
            return Pair.of(StringUtils.EMPTY, text);
        }

        int commandEnd = commandStart;
        while (commandEnd < textLength) {
            int codePoint = text.codePointAt(commandEnd);
            if (isSeparator(codePoint)) {
                break;
            }
            commandEnd += Character.charCount(codePoint);
        }

        String command = text.substring(commandStart, commandEnd);
        int contentStart = skipSeparatorsForward(text, commandEnd, textLength);
        int contentEnd = skipSeparatorsBackward(text, contentStart, textLength);
        String content = contentStart < contentEnd
                ? text.substring(contentStart, contentEnd)
                : StringUtils.EMPTY;

        return Pair.of(command, content);
    }

    public static boolean matches(String actualCommand, String expectedCommand) {
        if (StringUtils.isEmpty(actualCommand) || StringUtils.isEmpty(expectedCommand)) {
            return false;
        }
        if (actualCommand.equals(expectedCommand)) {
            return true;
        }

        int botUsernameSeparator = actualCommand.indexOf('@');
        return botUsernameSeparator > 0
                && actualCommand.substring(0, botUsernameSeparator).equals(expectedCommand);
    }

    private static int skipSeparatorsForward(String text, int start, int end) {
        int index = start;
        while (index < end) {
            int codePoint = text.codePointAt(index);
            if (!isSeparator(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }
        return index;
    }

    private static int skipSeparatorsBackward(String text, int start, int end) {
        int index = end;
        while (index > start) {
            int codePoint = text.codePointBefore(index);
            if (!isSeparator(codePoint)) {
                break;
            }
            index -= Character.charCount(codePoint);
        }
        return index;
    }

    private static boolean isSeparator(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || codePoint == ZERO_WIDTH_SPACE
                || codePoint == ZERO_WIDTH_NO_BREAK_SPACE;
    }
}
