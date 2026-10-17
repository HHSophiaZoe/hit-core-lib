package com.hit.chatbot;

import com.hit.chatbot.telegram.TelegramHtmlUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSplitterTest {

    @Test
    void shortOrEmptyTextIsNotSplit() {
        assertThat(MessageSplitter.splitText("abc", 10)).containsExactly("abc");
        assertThat(MessageSplitter.splitText("", 10)).isEmpty();
        assertThat(MessageSplitter.splitText(null, 10)).isEmpty();
    }

    @Test
    void textIsCutAtALineBreak() {
        List<String> parts = MessageSplitter.splitText("aaaa\nbbbb\ncccc\n", 11);

        assertThat(parts).containsExactly("aaaa\nbbbb\n", "cccc\n");
    }

    @Test
    void lineLongerThanTheLimitIsCutInside() {
        assertThat(MessageSplitter.splitText("abcdefghij", 4)).containsExactly("abcd", "efgh", "ij");
    }

    @Test
    void surrogatePairIsNeverCut() {
        String text = "abc😀def";
        List<String> parts = MessageSplitter.splitText(text, 4);

        assertThat(String.join("", parts)).isEqualTo(text);
        assertThat(parts).allSatisfy(part -> assertThat(Character.isHighSurrogate(part.charAt(part.length() - 1))).isFalse());
    }

    @Test
    void markdownCodeBlockCutInTwoStaysACodeBlock() {
        String text = "```\n" + "line\n".repeat(10) + "```";
        List<String> parts = MessageSplitter.splitMarkdown(text, 30);

        assertThat(parts).hasSizeGreaterThan(1).allSatisfy(part -> {
            assertThat(part.length()).isLessThanOrEqualTo(30);
            assertThat(part).startsWith("```").endsWith("```");
        });
    }

    @Test
    void htmlTagOpenAtACutIsClosedAndReopened() {
        // The first part is exactly 41 characters: opening tags, two lines, closing tags
        List<String> parts = TelegramHtmlUtils.split("<a href=\"https://x\"><b>aaaa\nbbbb\ncccc</b></a>", 41);

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0)).isEqualTo("<a href=\"https://x\"><b>aaaa\nbbbb\n</b></a>");
        assertThat(parts.get(1)).isEqualTo("<a href=\"https://x\"><b>cccc</b></a>");
        assertThat(parts).allSatisfy(part -> assertThat(part.length()).isLessThanOrEqualTo(41));
    }

    @Test
    void htmlIsNeverCutInsideATagOrAnEntity() {
        String html = "&lt;".repeat(10) + "<code>x</code>".repeat(5);
        for (String part : TelegramHtmlUtils.split(html, 9)) {
            assertThat(part.replaceAll("&lt;|</?code>|x", "")).as(part).isEmpty();
        }
    }

    @Test
    void shortHtmlIsNotSplit() {
        assertThat(TelegramHtmlUtils.split("<b>x</b>", 100)).containsExactly("<b>x</b>");
        assertThat(TelegramHtmlUtils.split(null, 100)).isEmpty();
    }
}
