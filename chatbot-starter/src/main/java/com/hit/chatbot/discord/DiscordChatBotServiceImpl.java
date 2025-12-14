package com.hit.chatbot.discord;

import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.data.request.DiscordMessageRequest;
import com.hit.chatbot.annotation.ConditionalOnDiscordEnable;
import com.hit.chatbot.data.request.MessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.awt.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnDiscordEnable
public class DiscordChatBotServiceImpl implements ChatBotService {

    private final JDA jda;

    @Override
    public void sendMessage(String chatId, String content) {
        TextChannel channel = this.getTextChannel(chatId);
        channel.sendMessage(content).queue(
                success -> log.info("Send message success to: {}", chatId),
                error -> log.error("Send message to {} failed: {}", chatId, error.getMessage())
        );
    }

    @Override
    public void sendMessage(MessageRequest request) {
        TextChannel channel = this.getTextChannel(request.getChatId());
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(request.getTitle())
                .setDescription(request.getContent())
                .setColor(this.colorOf(request.getType()));

        if (request instanceof DiscordMessageRequest discordMessageRequest) {
            if (ObjectUtils.isNotEmpty(discordMessageRequest.getFields())) {
                discordMessageRequest.getFields().forEach((key, value) -> embed.addField(key, value, false));
            }
        }

        MessageCreateAction messageCreateAction = channel.sendMessageEmbeds(embed.build());
        if (request instanceof DiscordMessageRequest discordMessageRequest) {
            if (CollectionUtils.isNotEmpty(discordMessageRequest.getFiles())) {
                discordMessageRequest.getFiles().forEach(file -> messageCreateAction.addFiles(FileUpload.fromData(file)));
            }
        }
        messageCreateAction.queue(
                success -> log.info("Send embed message success to: {}", request.getChatId()),
                error -> log.error("Send embed message to {} failed: {}", request.getChatId(), error.getMessage())
        );
    }

    @Override
    public void sendPrivateMessage(String userId, String content) {
        User user = jda.getUserById(userId);
        if (user == null) {
            log.error("Not found user: {}", userId);
            throw new IllegalArgumentException("User not found!");
        }

        user.openPrivateChannel().queue(
                privateChannel -> privateChannel.sendMessage(content).queue(
                        success -> log.info("Send private message success to: {}", userId),
                        error -> log.error("Send private message to {} failed: {}", userId, error.getMessage())
                ),
                error -> log.error("Don't open private channel: {}", error.getMessage(), error)
        );
    }

    private TextChannel getTextChannel(String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.error("Not found channel: {}", channelId);
            throw new IllegalArgumentException("Channel not found!");
        }
        return channel;
    }

    private Color colorOf(MessageRequest.MessageType type) {
        return switch (type) {
            case INFO -> Color.GREEN;
            case WARN -> Color.ORANGE;
            case ERROR -> Color.RED;
        };
    }
}
