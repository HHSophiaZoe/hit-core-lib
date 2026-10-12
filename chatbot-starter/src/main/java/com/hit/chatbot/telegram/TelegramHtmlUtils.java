package com.hit.chatbot.telegram;

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
}
