# WebSocket Client Starter

Thư viện WebSocket client dùng chung cho Java 21 và Spring Boot. Starter cung cấp:

- Kết nối và lifecycle thread-safe, không dùng lifecycle lock trong client.
- Reconnect có exponential backoff, jitter và giới hạn số lần thử.
- Native Ping/Pong hoặc heartbeat theo protocol ứng dụng.
- Dispatcher có partition, ordering và bounded queue.
- Connection pool theo capacity và reference count.
- Snapshot, lifecycle event và Micrometer metrics theo `source + connection`.
- Reactor Netty transport mặc định, có thể thay transport khác.

Lib không chứa auth, topic, subscription payload hay quy tắc quota của provider. Các phần đó thuộc adapter
của ứng dụng.

## Cài đặt

```xml
<dependency>
    <groupId>com.hit</groupId>
    <artifactId>websocket-client-starter</artifactId>
    <version>1.0</version>
</dependency>
```

Spring Boot tự tạo `WebSocketTransportFactory`, `WebSocketClientFactory`, dispatcher factory/registry,
`WebSocketConnectionPoolFactory` và các observer/query khi bật observability.

`spring.threads.virtual.enabled=true` khiến starter dùng virtual thread cho dispatcher worker và pool
coordinator. Adapter không cần truyền executor, thread type hay thread name.

## Khái niệm chính

```text
WebSocketClientFactory
└── WebSocketClient (một logical ConnectionId)
    ├── WebSocketTransport (một physical session tại một thời điểm)
    ├── reconnect + heartbeat + lifecycle
    └── WebSocketClientListener

MessageDispatcherRegistry
└── MessageDispatcher (ConnectionId + dispatcher name)
    └── N partition FIFO queues

WebSocketConnectionPool<R, C>
├── R: resource quota do adapter định nghĩa
└── C: physical connection do adapter triển khai
```

`ConnectionId(provider, name)` là ID logic ổn định, ví dụ `("dnse", "market-data-01")`; không dùng session
ID ngẫu nhiên của server. Một `WebSocketClientFactory` chỉ cho phép một client đang sống trên mỗi ID.

## 1. Kết nối không cần auth

```java
ConnectionId connectionId = new ConnectionId("market-provider", "quotes-01");
WebSocketConnectRequest request = WebSocketConnectRequest.of(
        URI.create("wss://provider.example/quotes"),
        Duration.ofSeconds(10));

WebSocketClientOptions options = WebSocketClientOptions.builder()
        .connectionId(connectionId)
        .connectRequest(request)
        .build();

WebSocketClient client = clientFactory.create(options, frame -> handle(frame));
client.connect();
```

Handshake thành công đưa client vào `CONNECTED`; không cần state xác thực giả lập.

`connect()`, `disconnect()`, `fail()` và `markPongReceived()` đưa command vào control loop rồi trả về. Không
giả định `state()` đổi đồng bộ ngay sau lời gọi. `connect()` không mở socket thứ hai khi client đang connect,
connected hoặc đợi retry.

## 2. Auth bằng HTTP header hoặc subprotocol

```java
WebSocketConnectRequest request = WebSocketConnectRequest.builder()
        .uri(URI.create("wss://provider.example/stream"))
        .headers(Map.of("Authorization", List.of("Bearer " + token)))
        .subProtocols(List.of("market-data-v1"))
        .connectTimeout(Duration.ofSeconds(10))
        .maxFramePayloadBytes(2 * 1024 * 1024)
        .build();
```

Không log request headers chứa credential. Khi token thay đổi, đóng client cũ và tạo client với request mới;
connect request là immutable.

## 3. Auth bằng message sau khi kết nối

Lib chỉ quản lý trạng thái transport. Adapter giữ `authenticated` và xử lý protocol riêng:

```java
public final class ProviderListener implements WebSocketClientListener {

    private final AtomicBoolean authenticated = new AtomicBoolean();
    private WebSocketClient client;

    void attach(WebSocketClient client) {
        this.client = client;
    }

    @Override
    public void onTransportConnected() {
        client.send(new WebSocketFrame.Text(authPayload()));
    }

    @Override
    public void onMessage(WebSocketFrame frame) {
        if (isAuthSuccess(frame)) {
            authenticated.set(true);
            resubscribeBusinessTopics();
            return;
        }
        handleBusinessMessage(frame);
    }

    @Override
    public void onTransportDisconnected(CloseReason reason) {
        authenticated.set(false);
    }
}
```

Welcome challenge, HMAC signature, auth timeout, subscription ACK và resubscribe là protocol của provider,
không thuộc starter. Nếu protocol không thể tiếp tục, adapter gọi:

```java
client.fail(new TransportFailure(
        FailureCategory.AUTHENTICATION,
        "AUTH_TIMEOUT",
        "Provider authentication timed out",
        true));
```

`fail(...)` kết thúc attempt hiện tại và có thể reconnect. `reportError(...)` chỉ ghi nhận lỗi, không đóng
connection.

## 4. Gửi và nhận text/binary frame

```java
client.send(new WebSocketFrame.Text("{\"action\":\"subscribe\"}"));
client.send(new WebSocketFrame.Binary(messagePackBytes));
```

```java
private void handle(WebSocketFrame frame) {
    switch (frame) {
        case WebSocketFrame.Text text -> handleJson(text.payload());
        case WebSocketFrame.Binary binary -> handleMessagePack(binary.payload());
        case WebSocketFrame.Ping ping -> handlePing(ping.payload());
        case WebSocketFrame.Pong pong -> handlePong(pong.payload());
    }
}
```

`send()` trả `CompletionStage<Void>`. Hoàn thành thành công chỉ có nghĩa transport đã nhận frame vào outbound
pipeline, không chứng minh server đã xử lý. Xác nhận nghiệp vụ phải dựa vào ACK của provider.

`inboundCapacity` mặc định 1024 frame/callback đang chờ. Hàng đợi inbound đầy làm attempt fail để bảo toàn
semantics; outbound Reactor Netty cũng có giới hạn 1024 frame.

## 5. Reconnect

Mặc định retry không giới hạn, delay 1–30 giây và jitter 20%.

### Tắt reconnect

```java
WebSocketClientOptions options = WebSocketClientOptions.builder()
        .connectionId(connectionId)
        .connectRequest(request)
        .reconnectPolicy(ReconnectPolicy.disabled())
        .build();
```

### Giới hạn số lần retry

```java
ReconnectPolicy policy = new ReconnectPolicy(
        5,
        Duration.ofSeconds(1),
        Duration.ofSeconds(30),
        0.2);
```

### Retry không giới hạn

```java
ReconnectPolicy policy = ReconnectPolicy.unlimited(
        Duration.ofSeconds(1),
        Duration.ofSeconds(30));
```

`maxAttempts`: `-1` là không giới hạn, `0` là tắt, số dương là số lần reconnect tối đa. Remote close có thể
reconnect nếu policy cho phép. `disconnect()`/`close()` chủ động không reconnect. HTTP 401/403 và lỗi TLS
không retry mặc định; 408/429/5xx có thể retry.

Backoff chỉ reset sau khi connection sống ổn định ít nhất 30 giây, tránh vòng lặp handshake thành công rồi
ngắt ngay làm mất retry budget.

## 6. Native WebSocket Ping/Pong

```java
WebSocketClientOptions options = WebSocketClientOptions.builder()
        .connectionId(connectionId)
        .connectRequest(request)
        .heartbeat(new HeartbeatOptions(
                Duration.ofSeconds(25),
                Duration.ofSeconds(10)))
        .build();
```

Khi chỉ cấu hình `HeartbeatOptions`, starter gửi `WebSocketFrame.Ping` chuẩn. Native pong hủy timeout và lên
lịch ping tiếp theo. Quá `pongTimeout` sẽ đóng attempt hiện tại và áp dụng reconnect policy. Heartbeat mặc
định tắt khi `heartbeat` là `null`.

## 7. Heartbeat theo protocol ứng dụng

Provider dùng JSON hoặc MessagePack ping có thể thay frame supplier:

```java
client.setHeartbeatFrameSupplier(() -> authenticated.get()
        ? new WebSocketFrame.Text("{\"action\":\"ping\"}")
        : null);
```

Khi decode được application pong:

```java
client.markPongReceived();
```

Supplier trả `null` để bỏ qua tick khi transport đã mở nhưng auth chưa xong. Supplier chạy trên control
thread nên phải nhanh và không block I/O.

## 8. Xử lý song song nhưng giữ thứ tự theo key

Lấy dispatcher theo cả connection và tên nghiệp vụ:

```java
MessageDispatcher dispatcher = dispatcherRegistry.get(connectionId, "market-data");
dispatcher.dispatch("trade", symbol, () -> processTrade(message));
```

- Cùng `orderingKey` luôn vào cùng partition và giữ FIFO.
- Key khác nhau có thể chạy song song.
- `metricCategory` chỉ dùng để nhóm metric hữu hạn, không đưa symbol vào đây.
- Capacity được áp dụng cho từng partition, không phải tổng dispatcher.

```yaml
websocket-client:
  dispatcher:
    defaults:
      partitions: 6
      queue-capacity: 10000
      overflow-policy: drop-oldest
    instances:
      market-data:
        partitions: 8
        queue-capacity: 20000
        overflow-policy: fail
```

| Policy | Hành vi khi queue đầy |
|---|---|
| `FAIL` | Ném lỗi cho caller |
| `DROP_LATEST` | Bỏ task mới |
| `DROP_OLDEST` | Bỏ task cũ nhất đang chờ rồi nhận task mới |

Không dùng drop policy nếu mọi message bắt buộc phải được xử lý. Registry do Spring quản lý tự đóng toàn bộ
dispatcher khi shutdown.

### Dispatcher với virtual thread

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Mỗi partition vẫn có đúng một worker tuần tự; virtual thread không tạo một task/thread cho từng message và
không thay đổi ordering hay concurrency. Nó hữu ích khi handler chờ I/O, không giúp tác vụ CPU chạy nhanh hơn.

Standalone có thể chọn thread factory:

```java
ThreadFactory threads = Thread.ofVirtual()
        .name("websocket-dispatcher-", 0)
        .factory();
MessageDispatcherFactory dispatcherFactory = new MessageDispatcherFactory(observers, threads);
```

## 9. Chia resource qua nhiều connection

Pool chỉ hiểu resource và capacity. Adapter tự định nghĩa một resource quota:

```java
record ChannelResource(String topic, String channel) {
}
```

Connection vật lý triển khai `PooledWebSocketConnection`, sau đó tạo pool:

```java
WebSocketConnectionPoolOptions options = new WebSocketConnectionPoolOptions(200, 2_000);
WebSocketConnectionPool<ChannelResource, ProviderConnection> pool =
        poolFactory.create(options, sequence -> createConnection(sequence));
```

Cấp resource và chỉ gửi protocol subscribe cho phần mới:

```java
PoolAllocation<ChannelResource> allocation = pool.acquire(requestedChannels);
allocation.added().forEach((id, added) -> sendSubscribe(pool.connection(id), added));
pool.connect(allocation.connectionIds());
```

Hủy resource:

```java
PoolRelease<ChannelResource> release = pool.release(requestedChannels);
release.removed().forEach((id, removed) -> sendUnsubscribe(pool.connection(id), removed));
pool.disconnect(release.unusedConnections());
```

Pool dùng first-fit và reference counting. Acquire trùng resource chỉ tăng reference; release chỉ trả resource
khi reference cuối cùng được đóng. Adapter chịu trách nhiệm encode subscribe/unsubscribe, auth, resubscribe
sau reconnect và gọi `maintainConnections()` theo policy ứng dụng.

Pool coordinator được starter chọn platform/virtual thread theo cấu hình Spring. Nó chỉ tuần tự hóa allocation;
không chạy callback provider trong coordinator để tránh deadlock khi callback gọi ngược vào pool.

## 10. Monitoring và metrics

```yaml
websocket-client:
  observability:
    enabled: true
    statistics-zone: Asia/Ho_Chi_Minh
    recent-event-capacity: 100
```

`statistics-zone` chỉ xác định ranh giới ngày cho counter process-local; event lưu `Instant`. Mặc định dùng
timezone hệ thống, không hard-code UTC. Có thể override bean `webSocketObservabilityClock` trong test/app.

```java
List<ConnectionSnapshot> connections = connectionSnapshotQuery.findBySource("dnse");
List<ConnectionEvent> events = connectionEventQuery.findRecent(connectionId, 50);
List<DispatcherSnapshot> dispatchers = dispatcherSnapshotQuery.findByConnection(connectionId);
```

Các query chỉ có bean mặc định khi observability bật. Snapshot và recent events nằm trong RAM của process.

### Metric connection

Mọi metric đều có tag low-cardinality `source` và `connection`.

| Metric | Ý nghĩa |
|---|---|
| `websocket.client.state` | STOPPED=0, CONNECTING=1, CONNECTED=2, RETRY_WAIT=3, DISCONNECTING=4, FAILED=5 |
| `websocket.client.events` | Lifecycle events theo loại event |
| `websocket.client.connections.attempted` | Số connect attempt |
| `websocket.client.connections.transport` | Số lần transport connected |
| `websocket.client.connect.duration` | Thời gian connect transport |
| `websocket.client.state.duration` | Thời gian ở từng state |
| `websocket.client.retries` | Số lần lên lịch reconnect |
| `websocket.client.disconnects` | Số disconnect chủ động |
| `websocket.client.connection.losses` | Unexpected close hoặc terminal failure |
| `websocket.client.heartbeat.timeouts` | Số pong timeout |
| `websocket.client.errors` | Lỗi theo category |
| `websocket.client.messages.sent/received` | Số frame transport chấp nhận/giao vào client |
| `websocket.client.bytes.sent/received` | Số byte tương ứng |
| `websocket.client.last.message.epoch.seconds` | Thời điểm nhận frame cuối |

### Metric dispatcher

| Metric | Ý nghĩa |
|---|---|
| `websocket.client.dispatch.queue.size` | Queue hiện tại theo partition |
| `websocket.client.dispatch.processed` | Task đã xử lý |
| `websocket.client.dispatch.duration` | Thời gian handler |
| `websocket.client.dispatch.dropped` | Task bị drop |
| `websocket.client.dispatch.errors` | Handler lỗi |

Starter chỉ metric frame/lifecycle. Muốn metric topic, channel, decoded message hay mapping/listener error,
adapter phải ghi nhận sau khi decode protocol; không đưa symbol hoặc payload động vào metric tag.

## 11. Observer tùy chỉnh

```java
@Bean
WebSocketObserver auditWebSocketObserver() {
    return new WebSocketObserver() {
        @Override
        public void onEvent(ConnectionEvent event) {
            audit(event);
        }
    };
}
```

Observer phải nhanh. Dữ liệu nghiệp vụ sau decode nên dùng observer riêng trong provider adapter thay vì ép
vào `WebSocketObserver` transport-neutral.

## 12. Thay transport

```java
final class CustomTransportFactory implements WebSocketTransportFactory {
    @Override
    public WebSocketTransport create(ConnectionId connectionId) {
        return new CustomTransport(connectionId);
    }
}
```

`CustomTransport` triển khai `connect(request, listener)`, `send(frame)`, `disconnect(reason)` và `isOpen()`.
Đăng ký một bean `WebSocketTransportFactory`; auto-configuration sẽ không tạo Reactor Netty factory mặc định.
Transport phải serialize outbound frames và tôn trọng contract bounded/non-blocking của `send()`.

## Shutdown và ownership

- Trong Spring, factory và registry được đóng qua bean lifecycle; adapter phải đóng pool nó đã tạo.
- Nếu tạo standalone, gọi `close()` cho client, dispatcher, pool và factory do mình sở hữu.
- `disconnect()` cho phép `connect()` lại; `close()` dừng vĩnh viễn.
- Callback phải ngắn, không block control thread và không chờ callback khác của cùng client.
- `WebSocketFrame.Binary/Ping/Pong` sao chép mảng byte ở biên API.

## Kiểm tra

```bash
./mvnw -f hit-core-lib/pom.xml -pl websocket-client-starter -am clean verify
```

Test dùng fake transport cho lifecycle và loopback server thật cho Ping/Pong, handshake, concurrent send,
cancel-connect, remote close và pooled-buffer ownership.
