package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.data.request.MessageRequest;
import com.hit.chatbot.data.request.TelegramMessageRequest;
import com.hit.chatbot.telegram.properties.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnTelegramEnable
public class TelegramChatBotServiceImpl extends DefaultLongPollingUpdateConsumer implements ChatBotService, SpringLongPollingBot {

    private final TelegramProperties telegramProperties;

    private final ChatBotMessageDispatcher dispatcher;

    private final TelegramClient telegramClient;

    @Override
    public String getBotToken() {
        return telegramProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        dispatcher.dispatchTelegramUpdate(update);
    }

    @Override
    public void sendMessage(String chatId, String content) {
        try {
            log.trace("Sending message to Telegram chatId: {}, message: {}", chatId, content);
            Pair<String, Integer> chatIdAndSubChatId = this.getChatIdAndSubChatId(chatId);
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatIdAndSubChatId.getLeft())
                    .messageThreadId(chatIdAndSubChatId.getRight())
                    .text(content)
                    .build();
            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chatId: {}", chatId, e);
        }
    }

    @Async
    @Override
    public void sendMessageAsync(String chatId, String content) {
        this.sendMessage(chatId, content);
    }

    @Override
    public void sendMessage(MessageRequest request) {
        try {
            log.trace("Sending message to Telegram: {}", request);
            Pair<String, Integer> chatIdAndSubChatId = this.getChatIdAndSubChatId(request.getChatId());
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatIdAndSubChatId.getLeft())
                    .messageThreadId(chatIdAndSubChatId.getRight())
                    .text(this.formatContent(request))
                    .build();

            if (request instanceof TelegramMessageRequest telegramRequest) {
                this.applyParseMode(sendMessage, telegramRequest.getParseMode());
                sendMessage.setDisableNotification(telegramRequest.isDisableNotification());
                sendMessage.setProtectContent(telegramRequest.isProtectContent());
            }

            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram", e);
        }
    }

    @Override
    public void sendPrivateMessage(String userId, String content) {
        throw new UnsupportedOperationException();
    }

    private Pair<String, Integer> getChatIdAndSubChatId(String chatId) {
        if (StringUtils.isBlank(chatId)) {
            return Pair.of(chatId, null);
        }

        String trimmed = chatId.trim();
        int firstShiftDash = trimmed.indexOf('_');
        if (firstShiftDash == -1) { // only chat id
            return Pair.of(trimmed, null);
        }

        String finalChatId = trimmed.substring(0, firstShiftDash);
        String subChatId = trimmed.substring(firstShiftDash + 1).trim();

        return Pair.of(finalChatId, Integer.parseInt(subChatId));
    }

    private void applyParseMode(SendMessage sendMessage, TelegramMessageRequest.ParseMode parseMode) {
        if (parseMode == null) {
            return;
        }
        switch (parseMode) {
            case HTML -> sendMessage.enableHtml(true);
            case MARKDOWN -> sendMessage.enableMarkdown(true);
            case MARKDOWN_V2 -> sendMessage.enableMarkdownV2(true);
        }
    }

    private String formatContent(MessageRequest request) {
        String title = request.getTitle();
        String content = request.getContent();

        if (!(request instanceof TelegramMessageRequest telegramRequest)) {
            return this.mergeContent(title, content);
        }

        TitleStyle titleStyle = TitleStyle.BOLD_DIVIDER; // ⭐ default đẹp nhất

        String renderedTitle = title != null
                ? this.renderTitle(title, titleStyle, telegramRequest.getParseMode())
                : null;

        return this.mergeContent(renderedTitle, content);
    }

    private String mergeContent(String title, String content) {
        if (StringUtils.isBlank(title)) {
            return content;
        }
        if (StringUtils.isBlank(content)) {
            return title;
        }
        return title + content;
    }

    private String renderTitle(String title, TitleStyle style, TelegramMessageRequest.ParseMode parseMode) {
        return switch (style) {

            case BOLD_DIVIDER -> {
                if (parseMode == null) {
                    yield """
                        %s
                        ───────────────
                        """.formatted(title);
                }
                yield switch (parseMode) {
                    case HTML -> """
                            <b>%s</b>
                            ───────────────
                            """.formatted(title);

                    case MARKDOWN, MARKDOWN_V2 -> """
                            *%s*
                            ───────────────
                            """.formatted(title);
                };
            }

            case EMOJI -> parseMode == null
                    ? "🚀 " + title
                    : switch (parseMode) {
                        case HTML -> "<b>🚀 %s</b>".formatted(title);
                        case MARKDOWN, MARKDOWN_V2 -> "*🚀 %s*".formatted(title);
                    };

            case BOX -> """
                    ╔══════════════════════╗
                    ║ %s ║
                    ╚══════════════════════╝
                    """.formatted(title);

            case HASH -> """
                    # %s
                    ───────────────
                    """.formatted(title);
        };
    }

    enum TitleStyle {
        BOLD_DIVIDER,
        EMOJI,
        BOX,
        HASH
    }
}
