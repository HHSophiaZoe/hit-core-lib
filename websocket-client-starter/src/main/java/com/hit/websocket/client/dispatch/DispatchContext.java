package com.hit.websocket.client.dispatch;

public record DispatchContext(String dispatcher, String metricCategory, int partition) {
}
