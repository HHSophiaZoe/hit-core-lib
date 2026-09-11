package com.hit.websocket.client.autoconfigure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@Setter
@Getter
@ConfigurationProperties("websocket-client.observability")
public class WebSocketClientObservabilityProperties {

    /** Enables snapshot, recent lifecycle events and Micrometer metrics. */
    private boolean enabled = false;

    /** Calendar zone used only for process-local daily snapshot counters. */
    private ZoneId statisticsZone = ZoneId.systemDefault();

    /** Maximum process-local lifecycle events retained for each connection. Zero disables retention. */
    private int recentEventCapacity = 0;

}
