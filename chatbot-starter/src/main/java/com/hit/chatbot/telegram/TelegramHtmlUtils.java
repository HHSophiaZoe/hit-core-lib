package com.hit.chatbot.telegram;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích cho tin Telegram gửi với {@code ParseMode.HTML}. Telegram chỉ hiểu một số tag và các entity
 * {@code &lt; &gt; &amp; &quot;} cùng entity dạng số; ký tự {@code < > &} lẻ làm hỏng cả tin.
 */
public final class TelegramHtmlUtils {

    /** Tag Telegram hỗ trợ, mở hoặc đóng, có thể kèm attribute (vd {@code <a href="...">}). */
    private static final Pattern SUPPORTED_TAG = Pattern.compile(
            "</?(b|strong|i|em|u|ins|s|strike|del|code|pre|a|span|tg-spoiler|tg-emoji|blockquote)(\\s[^<>]*)?>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TAG_NAME = Pattern.compile("^</?([a-zA-Z-]+)");

    /** Entity Telegram hỗ trợ. */
    private static final Pattern SUPPORTED_ENTITY = Pattern.compile("&(lt|gt|amp|quot|#\\d+|#x[0-9a-fA-F]+);");

    private TelegramHtmlUtils() {
    }

    /**
     * Escape text thuần để chèn vào tin HTML: chỉ thay {@code & < > "}, chữ có dấu và emoji giữ nguyên. Null → "".
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * Làm sạch HTML trước khi gửi: giữ tag và entity Telegram hỗ trợ, escape mọi {@code < > &} còn lại. Caller viết
     * HTML và nối thẳng text động (vd lỗi chứa {@code <}) mà không phải tự escape; text đã escape sẵn không bị escape
     * lần hai. Null → null.
     */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(html.length());
        Matcher tag = SUPPORTED_TAG.matcher(html);
        Matcher entity = SUPPORTED_ENTITY.matcher(html);
        int i = 0;
        while (i < html.length()) {
            char c = html.charAt(i);
            if (c == '<' && tag.region(i, html.length()).lookingAt()) {
                sanitized.append(tag.group());
                i = tag.end();
            } else if (c == '&' && entity.region(i, html.length()).lookingAt()) {
                sanitized.append(entity.group());
                i = entity.end();
            } else {
                switch (c) {
                    case '&' -> sanitized.append("&amp;");
                    case '<' -> sanitized.append("&lt;");
                    case '>' -> sanitized.append("&gt;");
                    default -> sanitized.append(c);
                }
                i++;
            }
        }
        return sanitized.toString();
    }

    /**
     * Splits sanitized HTML into parts of at most {@code limit} characters, preferably at a line break and never
     * inside a tag or an entity. A tag still open at a cut is closed at the end of the part and opened again, with its
     * attributes, at the start of the next one, so every part is valid HTML on its own. Null or empty gives an empty
     * list.
     */
    public static List<String> split(String html, int limit) {
        if (html == null || html.isEmpty()) {
            return List.of();
        }
        if (html.length() <= limit) {
            return List.of(html);
        }
        List<String> parts = new ArrayList<>();
        Deque<OpenTag> open = new ArrayDeque<>();
        StringBuilder body = new StringBuilder();
        String prefix = "";
        // Last line break in body, with the tags open there
        int breakAt = -1;
        List<OpenTag> openAtBreak = List.of();
        Matcher tag = SUPPORTED_TAG.matcher(html);
        Matcher entity = SUPPORTED_ENTITY.matcher(html);
        int i = 0;
        while (i < html.length()) {
            String token;
            if (html.charAt(i) == '<' && tag.region(i, html.length()).lookingAt()) {
                token = tag.group();
            } else if (html.charAt(i) == '&' && entity.region(i, html.length()).lookingAt()) {
                token = entity.group();
            } else {
                token = new String(Character.toChars(html.codePointAt(i)));
            }
            Deque<OpenTag> after = new ArrayDeque<>(open);
            apply(after, token);
            if (!body.isEmpty() && prefix.length() + body.length() + token.length() + closingLength(after) > limit) {
                if (breakAt > 0) {
                    parts.add(prefix + body.substring(0, breakAt) + closing(openAtBreak));
                    prefix = opening(openAtBreak);
                    body.delete(0, breakAt);
                } else {
                    parts.add(prefix + body + closing(open));
                    prefix = opening(open);
                    body.setLength(0);
                }
                breakAt = -1;
                continue;
            }
            body.append(token);
            open = after;
            if (token.equals("\n")) {
                breakAt = body.length();
                openAtBreak = List.copyOf(open);
            }
            i += token.length();
        }
        if (!body.isEmpty()) {
            parts.add(prefix + body);
        }
        return parts;
    }

    private record OpenTag(String name, String opening) {
    }

    private static void apply(Deque<OpenTag> open, String token) {
        if (!token.startsWith("<") || token.length() < 3) {
            return;
        }
        Matcher name = TAG_NAME.matcher(token);
        if (!name.find()) {
            return;
        }
        String tagName = name.group(1).toLowerCase(Locale.ROOT);
        if (token.startsWith("</")) {
            // Close the innermost tag of that name
            for (var it = open.iterator(); it.hasNext(); ) {
                if (it.next().name().equals(tagName)) {
                    it.remove();
                    return;
                }
            }
        } else {
            open.push(new OpenTag(tagName, token));
        }
    }

    private static String closing(Iterable<OpenTag> open) {
        StringBuilder closing = new StringBuilder();
        open.forEach(tag -> closing.append("</").append(tag.name()).append('>'));
        return closing.toString();
    }

    private static int closingLength(Iterable<OpenTag> open) {
        int length = 0;
        for (OpenTag tag : open) {
            length += tag.name().length() + 3;
        }
        return length;
    }

    private static String opening(List<OpenTag> open) {
        StringBuilder opening = new StringBuilder();
        for (int i = open.size() - 1; i >= 0; i--) {
            opening.append(open.get(i).opening());
        }
        return opening.toString();
    }

    private static String opening(Deque<OpenTag> open) {
        return opening(List.copyOf(open));
    }
}
