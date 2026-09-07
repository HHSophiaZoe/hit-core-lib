# WebSocket Client Starter

## Package structure

```text
com.hit.websocket.client
├── connection                  # Public connection lifecycle API and package-private manager
├── transport                   # Transport-neutral SPI and frame models
│   └── reactor                 # Reactor Netty adapter
├── dispatch                    # Partitioned dispatch API, registry and overflow policy
│   └── micrometer              # Dispatcher metrics adapter
├── observability               # Observer and query contracts
│   ├── model                   # Events and snapshots exposed to consumers
│   ├── memory                  # In-memory observability implementation
│   └── micrometer              # Connection metrics adapter
└── autoconfigure
    ├── properties              # Spring configuration properties
    └── condition               # Conditional bean annotations and conditions
```

Application code should depend on `connection`, `transport`, `dispatch` and observability contracts.
Reactor, Micrometer, memory storage and Spring auto-configuration are replaceable adapters.

Thư viện client dùng để kết nối từ service tới WebSocket server. Public API, lifecycle và
observability không phụ thuộc Reactor Netty; Reactor Netty chỉ là transport mặc định và có thể
được thay bằng Jetty hoặc OkHttp qua `WebSocketTransportFactory`.

## Thành phần

- `api`: transport-neutral frames, request, failure và transport port.
- `lifecycle`: connection state, reconnect, heartbeat và send serialization contract.
- `observability`: snapshot registry, lifecycle events và Micrometer adapter.
- `dispatch`: bounded partitioned dispatcher, ordering theo key và overflow policy.
- `transport.reactor`: Reactor Netty transport mặc định.
- `autoconfigure`: Spring Boot beans cho registry, metrics, lifecycle factory và transport mặc định.

## Tạo client

Inject `WebSocketClientManagerFactory`, sau đó tạo một logical connection:

```java
WebSocketClientOptions options = WebSocketClientOptions.builder()
        .connectionId(new ConnectionId("provider", "market-data"))
        .connectRequest(WebSocketConnectRequest.of(
                URI.create("wss://provider.example/ws"), Duration.ofSeconds(10)))
        .retryPolicy(RetryPolicy.unlimited(Duration.ofSeconds(1), Duration.ofSeconds(30)))
        .heartbeatInterval(Duration.ofSeconds(25))
        .pongTimeout(Duration.ofSeconds(10))
        .lifecycleStageTimeout(Duration.ofSeconds(10))
        .build();

ManagedWebSocketClient client = managerFactory.create(options, listener);
client.setHeartbeatFrameSupplier(() -> new WebSocketFrame.Text("{\"action\":\"ping\"}"));
client.connect();
```

Provider adapter cần gọi `markAuthenticating()`, `markResubscribing()` và `markReady()` để
snapshot phản ánh business readiness thay vì chỉ trạng thái transport.
`lifecycleStageTimeout` được khởi động lại khi vào `TRANSPORT_CONNECTED`, `AUTHENTICATING` và
`RESUBSCRIBING`; timeout sẽ đóng session bị treo và áp dụng retry policy. Heartbeat chỉ bắt đầu sau
`markReady()` để không can thiệp welcome/authentication handshake.
Lỗi nghiệp vụ làm connection không thể tiếp tục (ví dụ authentication bị từ chối) nên gọi
`fail(TransportFailure)` để đóng transport và áp dụng retry policy; `reportError(...)` chỉ ghi
nhận lỗi không làm gián đoạn connection.

Lifecycle event dùng các implementation typed của `ConnectionEventDetails` thay vì map key/value.
Application hoặc transport có thể bổ sung implementation riêng; `metadata` chỉ dành cho diagnostic
extension, không được dùng làm input cho state machine và không được chứa credential.

## Observability

Observability là opt-in và dùng một cờ duy nhất:

```yaml
websocket-client:
  observability:
    enabled: true
```

Khi tắt hoặc không cấu hình, starter không tạo snapshot registry và Micrometer observer; lifecycle,
reconnect và heartbeat vẫn hoạt động bình thường.

Khi có `MeterRegistry`, starter tự ghi các metric:

- `websocket.client.state` (`0=STOPPED`, `1=CONNECTING`, `2=TRANSPORT_CONNECTED`,
  `3=AUTHENTICATING`, `4=RESUBSCRIBING`, `5=READY`, `6=RETRY_WAIT`,
  `7=DISCONNECTING`, `8=FAILED`).
- `websocket.client.events`.
- `websocket.client.errors`.
- `websocket.client.messages.received`, `websocket.client.messages.sent`.
- `websocket.client.bytes.received`, `websocket.client.bytes.sent`.
- `websocket.client.connections.attempted`, `connections.transport`, `connections.ready`.
- `websocket.client.disconnects`, `connection.losses`, `retries`, `heartbeat.timeouts`.
- `websocket.client.connection.to.ready`, `transport.to.ready`, `state.duration`.
- `websocket.client.last.message.epoch.seconds` để metrics backend tính tuổi message cuối.

`ConnectionSnapshot.today` cung cấp counter process-local theo ngày cho operational API. Mặc định
dùng timezone hệ thống; application production nên đặt rõ `websocket-client.observability.statistics-zone`.
Counter này
không thay thế Prometheus hoặc persistent store và reset khi process restart.

Starter không tự public REST endpoint. Application inject `ConnectionSnapshotQuery` cho trạng thái
hiện tại và `ConnectionEventQuery` cho timeline lifecycle gần nhất để xây API theo response envelope,
authorization và nhu cầu dashboard của chính application. Không đưa credential, raw exception hoặc
URL có token vào `ConnectionEventDetails` hoặc metadata mở rộng.

Registry mặc định chỉ giữ snapshot và không lưu lifecycle event history. Đặt
`websocket-client.observability.recent-event-capacity` lớn hơn `0` để bật retention theo từng
connection; `0` tắt hoàn toàn và không cấp phát event buffer.
Nếu cần lịch sử bền vững, đẩy Kafka hoặc stream event lên UI, khai báo thêm `ConnectionObserver` hoặc
subscribe vào `ConnectionEventSource`; lifecycle, metric và transport không cần thay đổi.

Dashboard vẫn độc lập transport và không bị buộc vào schema HTTP của thư viện.

## Message dispatcher

`MessageDispatcher` tách business processing khỏi WebSocket receive thread. Các message có cùng
`orderingKey` luôn vào cùng một FIFO partition; các key khác nhau có thể được xử lý song song.

```yaml
websocket-client:
  dispatcher:
    defaults:
      partitions: 6
      queue-capacity: 10000
      overflow-policy: drop-oldest
    instances:
      dnse-tick:
        partitions: 8
        queue-capacity: 20000
      dnse-ohlc:
        partitions: 2
        overflow-policy: drop-latest
```

Mỗi instance chỉ cần khai báo field muốn override; field còn lại kế thừa từ `defaults`. Topic không có
entry trong `instances` vẫn có dispatcher riêng nhưng sử dụng toàn bộ giá trị từ `defaults`.
`queue-capacity` là dung lượng của từng partition; tổng số task tối đa xấp xỉ
`partitions * queue-capacity` cho mỗi dispatcher instance.
`MessageDispatcherRegistry` thực hiện việc khởi tạo lazy, cache và đóng toàn bộ dispatcher:

```java
MessageDispatcher dispatcher = dispatcherRegistry.get("market-data-tick");
dispatcher.dispatch("tick", symbol, () -> process(message));
```

Application tạo dispatcher riêng cho từng workload và chịu trách nhiệm cung cấp ordering key:

```java
MessageDispatcher dispatcher = dispatcherFactory.create(dispatcherProperties.getOptions("market-data"));
dispatcher.dispatch(symbol, () -> process(message));
```

Dispatcher chỉ cần `orderingKey` và handler để hoạt động. Overload nhận thêm `metricCategory` là
metadata tùy chọn để phân tách metrics theo loại business message; category không tham gia chọn
partition:

```java
dispatcher.dispatch("tick", symbol, () -> process(message));
```

Các policy gồm `BLOCK`, `DROP_LATEST`, `DROP_OLDEST` và `FAIL`. `BLOCK` có thể chặn caller nên không
nên dùng khi `dispatch()` được gọi trực tiếp trên WebSocket receive/event-loop thread. Không dùng policy drop cho order,
position hoặc event bắt buộc phải xử lý đầy đủ. Khi observability được bật, starter ghi queue size,
processed, dropped, errors và processing duration với tag `dispatcher`, `category`, `partition`.

## Thay transport

Khai báo một bean `WebSocketTransportFactory`. Auto-configuration sẽ không tạo Reactor Netty
factory khi đã có factory tùy chỉnh:

```java
@Bean
WebSocketTransportFactory webSocketTransportFactory() {
    return new JettyWebSocketTransportFactory(...);
}
```

Các provider adapter, metric, snapshot API và dashboard không cần thay đổi.

Scheduler retry/heartbeat chỉ chạy task ngắn và mặc định dùng 2 platform thread. Có thể đổi bằng
`websocket-client.scheduler.pool-size` hoặc khai báo bean `webSocketClientScheduler`; bật virtual
thread cho application không tự thay thế scheduler bean này.
