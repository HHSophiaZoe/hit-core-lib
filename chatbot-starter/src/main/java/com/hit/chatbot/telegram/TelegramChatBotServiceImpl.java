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

            // Format message with title
            String messageText = request.getTitle() != null ?
                    String.format("<b>%s</b>\n%s", request.getTitle(), request.getContent()) :
                    request.getContent();

            sendMessage.setText(messageText);

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
            return Pair.of(trimmed.substring(1), null);
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
}