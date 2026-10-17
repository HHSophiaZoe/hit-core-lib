package com.hit.chatbot;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a message that is longer than the limit of a platform into several messages, preferably at a line break,
 * never inside a surrogate pair. A markdown code block cut in two is closed at the end of a part and reopened at the
 * start of the next one, so both parts still render as code.
 */
public final class MessageSplitter {

    private static final String CODE_FENCE = "```";

    private MessageSplitter() {
    }

    /**
     * Parts of plain text, each at most {@code limit} characters. Null or empty gives an empty list.
     */
    public static List<String> splitText(String text, int limit) {
        return split(text, limit, false);
    }

    /**
     * Parts of markdown text (Discord), each at most {@code limit} characters, with code blocks kept valid.
     */
    public static List<String> splitMarkdown(String text, int limit) {
        return split(text, limit, true);
    }

    private static List<String> split(String text, int limit, boolean markdown) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (text.length() <= limit) {
            return List.of(text);
        }
        // Room for closing and reopening a code block around a cut
        int reserve = markdown ? CODE_FENCE.length() + 1 : 0;
        List<String> parts = new ArrayList<>();
        boolean inCode = false;
        int start = 0;
        while (start < text.length()) {
            String prefix = inCode ? CODE_FENCE + "\n" : "";
            int room = limit - prefix.length() - reserve;
            if (text.length() - start <= limit - prefix.length()) {
                parts.add(prefix + text.substring(start));
                break;
            }
            int end = cutPosition(text, start, start + room);
            String body = text.substring(start, end);
            boolean codeOpen = inCode ^ (countFences(body) % 2 == 1);
            parts.add(prefix + body + (markdown && codeOpen ? (body.endsWith("\n") ? "" : "\n") + CODE_FENCE : ""));
            inCode = markdown && codeOpen;
            start = end;
        }
        return parts;
    }

    /**
     * The end of a part that starts at {@code start}: after the last line break before {@code max} when there is one
     * in the second half, else {@code max}, moved back so a surrogate pair is not split.
     */
    private static int cutPosition(String text, int start, int max) {
        int newline = text.lastIndexOf('\n', max - 1);
        if (newline >= start + (max - start) / 2) {
            return newline + 1;
        }
        return Character.isLowSurrogate(text.charAt(max)) ? max - 1 : max;
    }

    private static int countFences(String text) {
        int count = 0;
        for (int i = text.indexOf(CODE_FENCE); i >= 0; i = text.indexOf(CODE_FENCE, i + CODE_FENCE.length())) {
            count++;
        }
        return count;
    }
}
