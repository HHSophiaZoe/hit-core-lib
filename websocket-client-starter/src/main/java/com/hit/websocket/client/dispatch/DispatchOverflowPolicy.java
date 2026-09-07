package com.hit.websocket.client.dispatch;

public enum DispatchOverflowPolicy {
    BLOCK,
    DROP_LATEST,
    DROP_OLDEST,
    FAIL
}
