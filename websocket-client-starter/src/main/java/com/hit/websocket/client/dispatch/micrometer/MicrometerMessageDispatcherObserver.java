package com.hit.websocket.client.dispatch.micrometer;

import com.hit.websocket.client.dispatch.DispatchContext;
import com.hit.websocket.client.dispatch.MessageDispatcherObserver;
import com.hit.websocket.client.connection.ConnectionId;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

public class MicrometerMessageDispatcherObserver implements MessageDispatcherObserver {

    private final MeterRegistry meterRegistry;

    public MicrometerMessageDispatcherObserver(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void queueRegistered(ConnectionId connectionId, String dispatcher, int partition, IntSupplier queueSize) {
        Gauge.builder("websocket.client.dispatch.queue.size", queueSize, IntSupplier::getAsInt)
                .strongReference(true)
                .tag("provider", connectionId.provider())
                .tag("connection", connectionId.name())
                .tag("dispatcher", dispatcher)
                .tag("partition", String.valueOf(partition))
                .register(meterRegistry);
    }

    @Override
    public void processed(DispatchContext context, Duration duration) {
        meterRegistry.counter("websocket.client.dispatch.processed", tags(context)).increment();
        meterRegistry.timer("websocket.client.dispatch.duration", tags(context))
                .record(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void dropped(DispatchContext context) {
        meterRegistry.counter("websocket.client.dispatch.dropped", tags(context)).increment();
    }

    @Override
    public void failed(DispatchContext context, Throwable error) {
        meterRegistry.counter("websocket.client.dispatch.errors", tags(context)).increment();
    }

    private String[] tags(DispatchContext context) {
        return new String[]{
                "provider", context.connectionId().provider(),
                "connection", context.connectionId().name(),
                "dispatcher", context.dispatcher(),
                "category", context.metricCategory(),
                "partition", String.valueOf(context.partition())
        };
    }
}
