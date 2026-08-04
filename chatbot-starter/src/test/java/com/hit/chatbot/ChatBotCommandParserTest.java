package com.hit.chatbot;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChatBotCommandParserTest {

    @ParameterizedTest
    @MethodSource("commandSeparators")
    void parsesAllCommonAsciiAndUnicodeSeparators(String separator) {
        Pair<String, String> result = ChatBotCommandParser.parse(
                separator + "/preview_rebalance" + separator + "my-tracker-01" + separator);

        assertThat(result.getLeft()).isEqualTo("/preview_rebalance");
        assertThat(result.getRight()).isEqualTo("my-tracker-01");
    }

    @Test
    void preservesWhitespaceInsideContent() {
        Pair<String, String> result = ChatBotCommandParser.parse(
                "/command\u00A0first  second\nthird\u3000");

        assertThat(result.getLeft()).isEqualTo("/command");
        assertThat(result.getRight()).isEqualTo("first  second\nthird");
    }

    @Test
    void handlesCommandWithoutContent() {
        assertThat(ChatBotCommandParser.parse("/command\u00A0\t"))
                .isEqualTo(Pair.of("/command", ""));
    }

    @Test
    void handlesNullAndSeparatorOnlyMessages() {
        assertThat(ChatBotCommandParser.parse(null)).isEqualTo(Pair.of("", null));
        assertThat(ChatBotCommandParser.parse("\u00A0\u3000\t"))
                .isEqualTo(Pair.of("", ""));
    }

    @Test
    void preservesNonCommandMessage() {
        String text = "  ordinary message  ";

        assertThat(ChatBotCommandParser.parse(text)).isEqualTo(Pair.of("", text));
    }

    @Test
    void matchesExactCommandAndTelegramBotUsernameSuffix() {
        assertThat(ChatBotCommandParser.matches("/command", "/command")).isTrue();
        assertThat(ChatBotCommandParser.matches("/command@my_bot", "/command")).isTrue();
        assertThat(ChatBotCommandParser.matches("/command_extra", "/command")).isFalse();
        assertThat(ChatBotCommandParser.matches("/command", "/command_extra")).isFalse();
    }

    private static Stream<Arguments> commandSeparators() {
        return Stream.of(
                Arguments.of(" "),                 // ASCII space
                Arguments.of("\t"),                // tab
                Arguments.of("\r\n"),              // line break
                Arguments.of("\u00A0"),            // no-break space
                Arguments.of("\u1680"),            // ogham space mark
                Arguments.of("\u2003"),            // em space
                Arguments.of("\u2009"),            // thin space
                Arguments.of("\u200B"),            // zero-width space
                Arguments.of("\u202F"),            // narrow no-break space
                Arguments.of("\u205F"),            // medium mathematical space
                Arguments.of("\u3000"),            // ideographic space
                Arguments.of("\uFEFF")             // zero-width no-break space/BOM
        );
    }
}
