package com.hit.websocket.client.dispatch;

import com.hit.websocket.client.connection.ConnectionId;

public record DispatchContext(ConnectionId connectionId, String dispatcher, String metricCategory, int partition) {
}
