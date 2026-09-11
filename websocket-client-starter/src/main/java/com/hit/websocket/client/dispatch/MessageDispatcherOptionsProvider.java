package com.hit.websocket.client.dispatch;

public interface MessageDispatcherOptionsProvider {

    MessageDispatcherOptions getOptions(String instanceName);
}
