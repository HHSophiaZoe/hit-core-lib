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
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnTelegramEnable
public class TelegramChatBotServiceImpl extends TelegramLongPollingBot implements ChatBotService {

    private final TelegramProperties telegramProperties;

    private final ChatBotMessageDispatcher dispatcher;

    @Override
    public String getBotUsername() {
        return telegramProperties.getUsername();
    }

    @Override
    public String getBotToken() {
        return telegramProperties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        dispatcher.dispatchTelegramUpdate(update);
    }

    @Override
    public void sendMessage(String chatId, String content) {
        try {
            log.debug("Sending message to Telegram chatId: {}, message: {}", chatId, content);
            SendMessage sendMessage = new SendMessage();
            Pair<String, Integer> chatIdAndSubChatId = this.getChatIdAndSubChatId(chatId);
            sendMessage.setChatId(chatIdAndSubChatId.getLeft());
            sendMessage.setMessageThreadId(chatIdAndSubChatId.getRight());
            sendMessage.setText(content);
            execute(sendMessage);
        } catch (Exception e) {
            log.warn("Failed to send message to Telegram chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendMessage(MessageRequest request) {
        try {
            log.info("Sending message to Telegram: {}", request);
            SendMessage sendMessage = new SendMessage();
            Pair<String, Integer> chatIdAndSubChatId = this.getChatIdAndSubChatId(request.getChatId());
            sendMessage.setChatId(chatIdAndSubChatId.getLeft());
            sendMessage.setMessageThreadId(chatIdAndSubChatId.getRight());
            sendMessage.setText(this.formatContent(request));

            if (request instanceof TelegramMessageRequest telegramRequest) {
                this.applyParseMode(sendMessage, telegramRequest.getParseMode());
                sendMessage.setDisableNotification(telegramRequest.isDisableNotification());
                sendMessage.setProtectContent(telegramRequest.isProtectContent());
            }

            execute(sendMessage);
            log.debug("Message sent successfully to Telegram: {}", request);
        } catch (Exception e) {
            log.warn("Failed to send message to Telegram", e);
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
            return merge(title, content);
        }

        TitleStyle titleStyle = TitleStyle.BOLD_DIVIDER; // ⭐ default đẹp nhất

        String renderedTitle = title != null
                ? renderTitle(title, titleStyle, telegramRequest.getParseMode())
                : null;

        return merge(renderedTitle, content);
    }

    private String merge(String title, String content) {
        if (title == null || title.isBlank()) {
            return content;
        }
        return title + content;
    }

    private String renderTitle(String title, TitleStyle style, TelegramMessageRequest.ParseMode parseMode) {
        return switch (style) {

            case BOLD_DIVIDER -> switch (parseMode) {
                case HTML -> """
                    <b>%s</b>
                    ────────────────────
                    """.formatted(title);

                case MARKDOWN, MARKDOWN_V2 -> """
                    *%s*
                    ────────────────────
                    """.formatted(title);

                default -> """
                    %s
                    ────────────────────
                    """.formatted(title);
            };

            case EMOJI -> switch (parseMode) {
                case HTML -> "<b>🚀 %s</b>".formatted(title);
                case MARKDOWN, MARKDOWN_V2 -> "*🚀 %s*".formatted(title);
                default -> "🚀 " + title;
            };

            case BOX -> """
                ╔══════════════════════╗
                ║ %s ║
                ╚══════════════════════╝
                """.formatted(title);

            case HASH -> """
                # %s
                ────────────────────
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