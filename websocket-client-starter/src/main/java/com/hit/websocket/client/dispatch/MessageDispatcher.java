package com.hit.websocket.client.dispatch;

public interface MessageDispatcher extends AutoCloseable {

    String UNSPECIFIED_CATEGORY = "unspecified";

    default void dispatch(String orderingKey, Runnable handler) {
        dispatch(UNSPECIFIED_CATEGORY, orderingKey, handler);
    }

    void dispatch(String metricCategory, String orderingKey, Runnable handler);

    void close();
}
