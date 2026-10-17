package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.MessageSplitter;
import com.hit.chatbot.data.request.MessageRequest;
import com.hit.chatbot.telegram.api.TelegramApiClient;
import com.hit.chatbot.telegram.api.TelegramSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

/**
 * Telegram bot. A {@link MessageRequest} is sent as HTML; plain {@code sendMessage(chatId, content)} as text. A message
 * longer than Telegram allows is sent as several messages. Failures are logged, never thrown, so a notification
 * cannot break the caller.
 */
@Slf4j
@RequiredArgsConstructor
public class TelegramChatBotServiceImpl implements ChatBotService {

    /**
     * Longest text of one Telegram message.
     */
    static final int MAX_MESSAGE_LENGTH = 4096;

    private static final String PARSE_MODE_HTML = "HTML";

    private static final String TITLE_DIVIDER = "\n───────────────\n";

    private final TelegramApiClient telegramClient;

    private final TaskExecutor taskExecutor;

    @Override
    public void sendMessage(String chatId, String content) {
        try {
            log.trace("Sending message to Telegram chatId: {}, message: {}", chatId, content);
            this.send(chatId, MessageSplitter.splitText(content, MAX_MESSAGE_LENGTH), null);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendMessageAsync(String chatId, String content) {
        taskExecutor.execute(() -> this.sendMessage(chatId, content));
    }

    @Override
    public void sendMessage(MessageRequest request) {
        try {
            log.trace("Sending message to Telegram: {}", request);
            this.send(request.getChatId(), TelegramHtmlUtils.split(this.toHtml(request), MAX_MESSAGE_LENGTH), PARSE_MODE_HTML);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chatId: {}", request.getChatId(), e);
        }
    }

    /**
     * Telegram has no separate private channel: the chat of a user is its user id, and the user must have started the
     * bot once.
     */
    @Override
    public void sendPrivateMessage(String userId, String content) {
        this.sendMessage(userId, content);
    }

    private void send(String chatId, List<String> parts, String parseMode) {
        Pair<String, Integer> chatIdAndSubChatId = this.getChatIdAndSubChatId(chatId);
        for (String part : parts) {
            telegramClient.sendMessage(TelegramSendMessage.builder()
                    .chatId(chatIdAndSubChatId.getLeft())
                    .messageThreadId(chatIdAndSubChatId.getRight())
                    .text(part)
                    .parseMode(parseMode)
                    .build());
        }
    }

    /**
     * The title, escaped and bold, above a divider, then the sanitized content.
     */
    private String toHtml(MessageRequest request) {
        String content = TelegramHtmlUtils.sanitize(request.getContent());
        if (StringUtils.isBlank(request.getTitle())) {
            return content;
        }
        String title = "<b>" + TelegramHtmlUtils.escape(request.getTitle()) + "</b>" + TITLE_DIVIDER;
        return StringUtils.isBlank(content) ? title : title + content;
    }

    /**
     * {@code chatId_threadId} for a topic of a forum group, else the chat id alone.
     */
    private Pair<String, Integer> getChatIdAndSubChatId(String chatId) {
        if (StringUtils.isBlank(chatId)) {
            return Pair.of(chatId, null);
        }
        String trimmed = chatId.trim();
        int separator = trimmed.indexOf('_');
        if (separator == -1) {
            return Pair.of(trimmed, null);
        }
        return Pair.of(trimmed.substring(0, separator), Integer.parseInt(trimmed.substring(separator + 1).trim()));
    }
}
