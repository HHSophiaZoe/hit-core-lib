package com.hit.websocket.client.autoconfigure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("websocket-client.observability")
public class WebSocketClientObservabilityProperties {

    /** Enables snapshot, recent lifecycle events and Micrometer metrics. */
    private boolean enabled = false;

    /** Maximum process-local lifecycle events retained for each connection. Zero disables retention. */
    private int recentEventCapacity = 0;

}
