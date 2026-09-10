package com.hit.websocket.client.pool;

public record WebSocketConnectionPoolOptions(
        int maxResourcesPerConnection,
        int maxTotalResources
) {

    public WebSocketConnectionPoolOptions {
        if (maxResourcesPerConnection < 1 || maxTotalResources < maxResourcesPerConnection) {
            throw new IllegalArgumentException(
                    "Connection pool limits must satisfy 0 < maxResourcesPerConnection <= maxTotalResources");
        }
    }
}
