package com.hit.websocket.client.autoconfigure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("websocket-client.scheduler")
public class WebSocketClientSchedulerProperties {

    /** Platform threads used only for short retry and heartbeat scheduling tasks. */
    private int poolSize = 2;
}
