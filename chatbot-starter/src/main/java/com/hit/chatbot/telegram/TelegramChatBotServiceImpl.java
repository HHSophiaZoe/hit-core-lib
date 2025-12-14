package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.data.request.MessageRequest;
import com.hit.chatbot.data.request.TelegramMessageRequest;
import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.telegram.properties.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
            sendMessage.setChatId(chatId);
            sendMessage.setText(content);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to Telegram chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendMessage(MessageRequest request) {
        try {
            log.info("Sending message to Telegram: {}", request);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(request.getChannelId());
            
            // Format message với title nếu có
            String messageText = request.getTitle() != null ? 
                String.format("<b>%s</b>\n%s", request.getTitle(), request.getContent()) :
                request.getContent();
            
            sendMessage.setText(messageText);
            
            if (request instanceof TelegramMessageRequest telegramRequest) {
                if (telegramRequest.getParseMode() != null) {
                    sendMessage.setParseMode(telegramRequest.getParseMode());
                } else {
                    sendMessage.setParseMode("HTML"); // Default
                }
                sendMessage.setDisableNotification(telegramRequest.isDisableNotification());
                sendMessage.setProtectContent(telegramRequest.isProtectContent());
            } else {
                sendMessage.setParseMode("HTML");
            }
            
            execute(sendMessage);
            log.info("Message sent successfully to Telegram");
        } catch (TelegramApiException e) {
            log.error("Failed to send message to Telegram", e);
        }
    }

    @Override
    public void sendPrivateMessage(String userId, String content) {
        try {
            log.info("Sending private message to Telegram user: {}", userId);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(userId);
            sendMessage.setText(content);
            execute(sendMessage);
            log.info("Private message sent successfully to user: {}", userId);
        } catch (TelegramApiException e) {
            log.error("Failed to send private message to Telegram user: {}", userId, e);
        }
    }
}