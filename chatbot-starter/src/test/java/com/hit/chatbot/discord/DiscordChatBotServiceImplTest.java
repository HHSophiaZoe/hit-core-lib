package com.hit.chatbot.discord;

import com.hit.chatbot.data.request.DiscordMessageRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.invocation.Invocation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * JDA is mocked: the tests check what the service asks Discord to send.
 */
class DiscordChatBotServiceImplTest {

    private final JDA jda = mock(JDA.class);

    private final TextChannel channel = mock(TextChannel.class, RETURNS_DEEP_STUBS);

    private final List<Runnable> executed = new CopyOnWriteArrayList<>();

    private final DiscordChatBotServiceImpl service = new DiscordChatBotServiceImpl(jda, task -> {
        executed.add(task);
        task.run();
    }, Duration.ofMillis(300));

    DiscordChatBotServiceImplTest() {
        when(jda.getStatus()).thenReturn(JDA.Status.CONNECTED);
        when(jda.getTextChannelById("c")).thenReturn(channel);
    }

    @Test
    void longContentIsSentAsSeveralMessages() {
        String content = ("x".repeat(99) + "\n").repeat(45);

        service.sendMessage("c", content);

        List<String> parts = calls(channel, "sendMessage").stream().map(args -> args[0].toString()).toList();
        assertThat(parts).hasSize(3).allSatisfy(part -> assertThat(part.length()).isLessThanOrEqualTo(Message.MAX_CONTENT_LENGTH));
        assertThat(String.join("", parts)).isEqualTo(content);
    }

    @Test
    void longEmbedContinuesInFurtherEmbeds(@TempDir Path dir) throws Exception {
        File file = Files.writeString(dir.resolve("report.txt"), "data").toFile();
        String description = ("y".repeat(99) + "\n").repeat(90);

        service.sendMessage(DiscordMessageRequest.builder()
                .chatId("c")
                .title("Report")
                .content(description)
                .fields(Map.of("Total", "9"))
                .files(List.of(file))
                .build());

        List<MessageEmbed> embeds = calls(channel, "sendMessageEmbeds").stream().map(args -> (MessageEmbed) args[0]).toList();
        assertThat(embeds).hasSize(3);
        assertThat(embeds.getFirst().getTitle()).isEqualTo("Report");
        assertThat(embeds.getFirst().getFields()).hasSize(1);
        assertThat(embeds.subList(1, 3)).allSatisfy(embed -> {
            assertThat(embed.getTitle()).isNull();
            assertThat(embed.getFields()).isEmpty();
        });
        assertThat(embeds).allSatisfy(embed ->
                assertThat(embed.getDescription().length()).isLessThanOrEqualTo(MessageEmbed.DESCRIPTION_MAX_LENGTH));
    }

    @Test
    void unknownChannelIsLoggedNotThrown() {
        service.sendMessage("missing", "x");
        service.sendMessage(DiscordMessageRequest.builder().chatId("missing").content("x").build());

        assertThat(mockingDetails(channel).getInvocations()).isEmpty();
    }

    @Test
    void sendWaitsWhileTheBotIsConnecting() {
        when(jda.getStatus()).thenReturn(JDA.Status.LOADING_SUBSYSTEMS, JDA.Status.LOADING_SUBSYSTEMS, JDA.Status.CONNECTED);

        service.sendMessage("c", "x");

        assertThat(calls(channel, "sendMessage")).hasSize(1);
    }

    @Test
    void sendGivesUpWhenTheBotDoesNotConnectInTime() {
        when(jda.getStatus()).thenReturn(JDA.Status.LOADING_SUBSYSTEMS);
        long start = System.nanoTime();

        service.sendMessage("c", "x");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isBetween(Duration.ofMillis(250), Duration.ofSeconds(2));
        assertThat(mockingDetails(channel).getInvocations()).isEmpty();
    }

    @Test
    void failedLoginDoesNotWait() {
        when(jda.getStatus()).thenReturn(JDA.Status.FAILED_TO_LOGIN);
        long start = System.nanoTime();

        service.sendMessage("c", "x");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(200));
        assertThat(mockingDetails(channel).getInvocations()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void privateMessageOpensTheChannelThroughTheApi() {
        CacheRestAction<PrivateChannel> open = mock(CacheRestAction.class);
        PrivateChannel privateChannel = mock(PrivateChannel.class, RETURNS_DEEP_STUBS);
        when(jda.openPrivateChannelById("u")).thenReturn(open);
        doAnswer(invocation -> {
            invocation.<Consumer<PrivateChannel>>getArgument(0).accept(privateChannel);
            return null;
        }).when(open).queue(any(), any());

        service.sendPrivateMessage("u", "hello");

        assertThat(calls(privateChannel, "sendMessage")).singleElement()
                .satisfies(args -> assertThat(args[0].toString()).isEqualTo("hello"));
    }

    @Test
    void asyncSendRunsOnTheTaskExecutor() {
        service.sendMessageAsync("c", "x");

        assertThat(executed).hasSize(1);
        assertThat(calls(channel, "sendMessage")).hasSize(1);
    }

    private static List<Object[]> calls(Object mock, String method) {
        return mockingDetails(mock).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(method))
                .map(Invocation::getArguments)
                .toList();
    }
}
