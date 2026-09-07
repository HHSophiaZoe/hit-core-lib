package com.hit.websocket.client.autoconfigure.properties;

import com.hit.websocket.client.dispatch.DispatchOverflowPolicy;
import com.hit.websocket.client.dispatch.MessageDispatcherOptions;
import com.hit.websocket.client.dispatch.MessageDispatcherOptionsProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "websocket-client.dispatcher")
public class WebSocketClientDispatcherProperties implements MessageDispatcherOptionsProvider {

    private DispatcherSettings defaults = DispatcherSettings.defaults();
    private Map<String, DispatcherSettings> instances = new LinkedHashMap<>();

    @Override
    public MessageDispatcherOptions getOptions(String name) {
        DispatcherSettings settings = resolveSettings(name);
        return new MessageDispatcherOptions(
                name,
                settings.getPartitions(),
                settings.getQueueCapacity(),
                settings.getOverflowPolicy()
        );
    }

    private DispatcherSettings resolveSettings(String name) {
        DispatcherSettings configured = instances.get(name);
        if (configured == null) {
            return defaults;
        }

        return new DispatcherSettings()
                .setPartitions(orDefault(configured.getPartitions(), defaults.getPartitions()))
                .setQueueCapacity(orDefault(configured.getQueueCapacity(), defaults.getQueueCapacity()))
                .setOverflowPolicy(orDefault(configured.getOverflowPolicy(), defaults.getOverflowPolicy()));
    }

    private <T> T orDefault(T configuredValue, T defaultValue) {
        return configuredValue != null ? configuredValue : defaultValue;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class DispatcherSettings {

        private Integer partitions;
        private Integer queueCapacity;
        private DispatchOverflowPolicy overflowPolicy;

        private static DispatcherSettings defaults() {
            return new DispatcherSettings()
                    .setPartitions(6)
                    .setQueueCapacity(10_000)
                    .setOverflowPolicy(DispatchOverflowPolicy.DROP_OLDEST);
        }
    }

}
