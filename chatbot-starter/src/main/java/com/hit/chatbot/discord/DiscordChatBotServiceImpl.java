package com.hit.chatbot.discord;

import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.MessageSplitter;
import com.hit.chatbot.data.request.DiscordMessageRequest;
import com.hit.chatbot.data.request.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;

import java.awt.Color;
import java.time.Duration;
import java.util.List;

/**
 * Discord bot. Content is Discord markdown (HTML is not supported). A message longer than Discord allows is sent as
 * several messages. Failures are logged, never thrown, so a notification cannot break the caller.
 */
@Slf4j
public class DiscordChatBotServiceImpl implements ChatBotService {

    private static final Duration READY_POLL = Duration.ofMillis(100);

    private final JDA jda;

    private final TaskExecutor taskExecutor;

    private final Duration readyTimeout;

    /**
     * @param readyTimeout how long a send waits for the bot to finish connecting at startup
     */
    public DiscordChatBotServiceImpl(JDA jda, TaskExecutor taskExecutor, Duration readyTimeout) {
        this.jda = jda;
        this.taskExecutor = taskExecutor;
        this.readyTimeout = readyTimeout;
    }

    @Override
    public void sendMessage(String chatId, String content) {
        try {
            TextChannel channel = this.getTextChannel(chatId);
            if (channel == null) {
                return;
            }
            for (String part : MessageSplitter.splitMarkdown(content, Message.MAX_CONTENT_LENGTH)) {
                channel.sendMessage(part).queue(
                        success -> log.debug("Send message success to: {}", chatId),
                        error -> log.error("Send message to {} failed: {}", chatId, error.getMessage(), error));
            }
        } catch (Exception e) {
            log.error("Failed to send message to Discord channel: {}", chatId, e);
        }
    }

    @Override
    public void sendMessageAsync(String chatId, String content) {
        taskExecutor.execute(() -> this.sendMessage(chatId, content));
    }

    /**
     * Sends an embed: title, colour by type, description, then the fields and files of a {@link DiscordMessageRequest}.
     * A description longer than an embed allows continues in further embeds.
     */
    @Override
    public void sendMessage(MessageRequest request) {
        try {
            TextChannel channel = this.getTextChannel(request.getChatId());
            if (channel == null) {
                return;
            }
            Color color = this.colorOf(request.getType());
            List<String> descriptions = MessageSplitter.splitMarkdown(request.getContent(), MessageEmbed.DESCRIPTION_MAX_LENGTH);
            int count = Math.max(descriptions.size(), 1);
            for (int i = 0; i < count; i++) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(color)
                        .setDescription(descriptions.isEmpty() ? null : descriptions.get(i));
                if (i == 0) {
                    embed.setTitle(StringUtils.abbreviate(request.getTitle(), MessageEmbed.TITLE_MAX_LENGTH));
                    this.addFields(embed, request);
                }
                MessageCreateAction action = channel.sendMessageEmbeds(embed.build());
                if (i == 0) {
                    this.addFiles(action, request);
                }
                action.queue(
                        success -> log.debug("Send embed message success to: {}", request.getChatId()),
                        error -> log.error("Send embed message to {} failed: {}", request.getChatId(), error.getMessage(), error));
            }
        } catch (Exception e) {
            log.error("Failed to send embed message to Discord channel: {}", request.getChatId(), e);
        }
    }

    /**
     * Opens the direct message channel through the API, so the user needs not be in the bot's cache.
     */
    @Override
    public void sendPrivateMessage(String userId, String content) {
        try {
            if (!this.awaitReady()) {
                return;
            }
            jda.openPrivateChannelById(userId).queue(
                    privateChannel -> MessageSplitter.splitMarkdown(content, Message.MAX_CONTENT_LENGTH)
                            .forEach(part -> privateChannel.sendMessage(part).queue(
                                    success -> log.debug("Send private message success to: {}", userId),
                                    error -> log.error("Send private message to {} failed: {}", userId, error.getMessage(), error))),
                    error -> log.error("Cannot open private channel with {}: {}", userId, error.getMessage(), error));
        } catch (Exception e) {
            log.error("Failed to send private message to Discord user: {}", userId, e);
        }
    }

    private TextChannel getTextChannel(String channelId) {
        if (!this.awaitReady()) {
            return null;
        }
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.error("Discord channel not found: {}", channelId);
        }
        return channel;
    }

    /**
     * Waits, up to {@code readyTimeout}, while the bot is still connecting: the channels are only known once it is
     * connected. A failed login or a shut down bot does not wait.
     */
    private boolean awaitReady() {
        long deadline = System.nanoTime() + readyTimeout.toNanos();
        while (connecting(jda.getStatus()) && System.nanoTime() < deadline) {
            try {
                Thread.sleep(READY_POLL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        JDA.Status status = jda.getStatus();
        if (connecting(status) || status == JDA.Status.FAILED_TO_LOGIN || status == JDA.Status.SHUTTING_DOWN
                || status == JDA.Status.SHUTDOWN) {
            log.error("Discord bot is not connected ({}), message dropped", status);
            return false;
        }
        return true;
    }

    /**
     * Still logging in or loading; JDA counts CONNECTED among its init statuses too, so it is excluded.
     */
    private static boolean connecting(JDA.Status status) {
        return status.isInit() && status != JDA.Status.CONNECTED;
    }

    private void addFields(EmbedBuilder embed, MessageRequest request) {
        if (request instanceof DiscordMessageRequest discordRequest && MapUtils.isNotEmpty(discordRequest.getFields())) {
            discordRequest.getFields().forEach((name, value) -> embed.addField(
                    StringUtils.abbreviate(name, MessageEmbed.TITLE_MAX_LENGTH),
                    StringUtils.abbreviate(value, MessageEmbed.VALUE_MAX_LENGTH), false));
        }
    }

    private void addFiles(MessageCreateAction action, MessageRequest request) {
        if (request instanceof DiscordMessageRequest discordRequest && CollectionUtils.isNotEmpty(discordRequest.getFiles())) {
            discordRequest.getFiles().forEach(file -> action.addFiles(FileUpload.fromData(file)));
        }
    }

    private Color colorOf(MessageRequest.MessageType type) {
        if (type == null) {
            return Color.GREEN;
        }
        return switch (type) {
            case INFO -> Color.GREEN;
            case WARN -> Color.ORANGE;
            case ERROR -> Color.RED;
        };
    }
}
