# WebSocket Client Starter

Java 21 / Spring Boot. Bắt đầu đọc từ `connection/WebSocketClient.java`,
`connection/WebSocketClientOptions.java` và `connection/DefaultWebSocketClient.java`.

## Dùng ngay, không cần auth

Inject `WebSocketClientFactory`:

```java
WebSocketClient client = factory.create(
        WebSocketClientOptions.builder()
                .connectionId(new ConnectionId("provider", "quotes"))
                .connectRequest(WebSocketConnectRequest.of(
                        URI.create("wss://provider.example/quotes"), Duration.ofSeconds(10)))
                .build(),
        frame -> dispatcher.dispatch("quotes", () -> process(frame)));
client.connect();
```

Handshake thành công là `CONNECTED`. Không cần gọi markReady hoặc khai báo bước authentication.
Headers, subprotocols và giới hạn frame cấu hình bằng `WebSocketConnectRequest.builder()`.
Transport có thể thay bằng một bean `WebSocketTransportFactory`.

`connect()`, `disconnect()`, `fail()`, `markPongReceived()` đưa yêu cầu vào luồng điều khiển rồi trả về.
Đọc `state()` hoặc nhận callback để quan sát kết quả; không giả định state đổi ngay sau lời gọi.
`connect()` không mở thêm socket nếu đang connecting/connected/retry-wait.
Gọi `connect()` ở FAILED bắt đầu lại với retry budget mới.

`disconnect()` dừng lifecycle, hủy reconnect/heartbeat và yêu cầu đóng transport.
Có thể gọi connect lại sau disconnect. `close()` dừng vĩnh viễn và giải phóng luồng điều khiển;
factory do Spring quản lý tự close toàn bộ client lúc shutdown.
Callback phải ngắn, không chờ I/O hay chờ callback khác trên cùng client.
Close socket của Reactor có giới hạn 3 giây; state STOPPED biểu thị supervisor đã dừng,
không phải xác nhận peer đã nhận close frame.

## Reconnect

Mặc định retry không giới hạn, backoff từ 1 đến 30 giây với jitter 20%.
Dùng `ReconnectPolicy.disabled()` để tắt hoặc cấu hình số lần retry cụ thể:

```java
ReconnectPolicy policy = new ReconnectPolicy(5, Duration.ofSeconds(1), Duration.ofSeconds(30), 0.2);
```

Cả remote close bình thường và lỗi retryable đều có thể reconnect. Disconnect/close chủ động không retry.
Lỗi không retryable hoặc hết budget chuyển FAILED. Backoff chỉ reset sau khi một connection tồn tại
ít nhất 30 giây, tránh vòng lặp handshake thành công rồi ngắt ngay làm mất giới hạn retry.
HTTP handshake 401/403 và lỗi TLS không retry mặc định; 408/429/5xx có retry.
Adapter có thể báo lỗi nghiệp vụ bằng `fail(TransportFailure)`; `reportError(...)` chỉ ghi nhận lỗi.

## Heartbeat

Heartbeat mặc định tắt. Cấu hình `heartbeat(new HeartbeatOptions(interval, pongTimeout))`
để bật WebSocket Ping/Pong chuẩn. Pong đến sẽ hủy deadline và lên lịch ping tiếp theo;
timeout đóng connection và áp dụng reconnect policy.

Protocol dùng JSON/MessagePack ping có thể gọi:

```java
client.setHeartbeatFrameSupplier(() -> new WebSocketFrame.Text("{\"action\":\"ping\"}"));
// Trong onMessage khi nhận application pong:
client.markPongReceived();
```

Supplier trả null để bỏ qua tick, ví dụ khi ứng dụng chưa auth xong. Supplier chạy trên luồng
điều khiển nên phải nhanh. Các timer đo thời gian trôi qua bằng monotonic clock, không phụ thuộc timezone.

## Auth và nghiệp vụ

Auth, subscription ACK và readiness nghiệp vụ thuộc adapter ứng dụng. Adapter gửi auth từ
`onTransportConnected()` hoặc sau welcome; xử lý kết quả trong `onMessage()`.
Lib không có state AUTHENTICATING, RESUBSCRIBING hay READY. DNSE tự giữ `authenticated`,
đánh dấu business-ready sau resubscribe và tự kiểm tra timeout welcome/auth.

Callback transport cũ và các command đã xếp hàng cho attempt cũ bị bỏ qua sau reconnect.
Nếu ứng dụng tự chạy công việc bất đồng bộ bên ngoài callback, ứng dụng phải giữ/cancel context
nghiệp vụ của session đó; không dùng kết quả auth cũ để gọi fail/send trên connection mới.

## Dispatcher

`MessageDispatcher` dùng một single-worker executor của JDK cho mỗi partition.
Cùng ordering key vào cùng FIFO queue; các key khác nhau có thể chạy song song.
Worker mặc định là platform thread. Trên JDK 21, Spring auto-configuration đọc
`spring.threads.virtual.enabled=true` để dùng virtual worker cho dispatcher.
Standalone có thể truyền `Thread.ofVirtual().factory()` vào constructor của `MessageDispatcherFactory`.
Đây vẫn là một consumer tuần tự mỗi partition, không phải virtual thread cho từng message:
giới hạn concurrency và ordering không thay đổi. Virtual thread hữu ích khi handler chờ I/O,
không làm tác vụ CPU chạy nhanh hơn. Lifecycle timer và Reactor Netty event loop không đổi thread type.
Không chờ I/O bên trong `synchronized` của handler trên JDK 21 để tránh pinning carrier thread.

Submission và shutdown được bảo vệ bằng một `ReentrantLock` ngắn mỗi partition.
Khi DROP_OLDEST, bỏ task cũ rồi submit lại đúng một lần, không có vòng retry vô hạn.
Handler, log và observer không chạy trong khóa này.
Queue có giới hạn cho từng partition:

```yaml
websocket-client:
  dispatcher:
    defaults:
      partitions: 6
      queue-capacity: 10000
      overflow-policy: drop-oldest
    instances:
      dnse-tick:
        queue-capacity: 20000
```

```java
MessageDispatcher dispatcher = dispatcherRegistry.get("dnse-tick");
dispatcher.dispatch("tick", symbol, () -> process(message));
```

Policy: FAIL ném lỗi, DROP_LATEST bỏ task mới, DROP_OLDEST bỏ task đang chờ cũ nhất.
Không dùng policy drop cho dữ liệu bắt buộc xử lý đầy đủ. BLOCK đã bỏ để tránh chặn receive thread.
Close từ chối task mới, bỏ task chờ và interrupt worker; handler phải hợp tác với interruption.

## Connection pool

`WebSocketConnectionPool` phân bổ các resource quota do adapter định nghĩa vào nhiều physical connection.
Pool dùng first-fit, giới hạn capacity và reference counting; nó không biết auth, topic hay payload của provider.
Adapter triển khai `PooledWebSocketConnection` và cung cấp factory tạo connection theo sequence.

`WebSocketConnectionPoolFactory` do lib auto-configure và tự chọn virtual hoặc daemon platform coordinator
từ `spring.threads.virtual.enabled`. Adapter không truyền thread name, thread type hay tự quản lý `ExecutorService`.
Coordinator chỉ tuần tự hóa thay đổi allocation. Callback và lifecycle của provider chạy bên ngoài coordinator
để có thể gọi ngược lại pool mà không deadlock.

Lib cũng giới hạn frame đang chờ callback và send completion (mặc định 1024 cho mỗi loại,
cấu hình bằng `inboundCapacity`). Inbound đầy sẽ fail/reconnect; pending send đầy trả failed stage.
Outbound Reactor giới hạn 1024 frame. Send thành công chỉ có nghĩa frame đã được nhận vào pipeline,
không phải server đã nhận/xử lý; ACK nghiệp vụ thuộc ứng dụng.

## Metrics và Clock

Observability bật theo nhu cầu:

```yaml
websocket-client:
  observability:
    enabled: true
    statistics-zone: Asia/Ho_Chi_Minh
    recent-event-capacity: 100
```

`WebSocketObserver` nhận lifecycle/traffic; có adapter Micrometer và snapshot/event history trong RAM.
Lỗi observer được cô lập. Lifecycle và listener chạy tuần tự trên một control thread riêng cho mỗi client.
Không có lock trong DefaultWebSocketClient. Lock nhỏ ở registry/factory bảo vệ create/close;
Micrometer và snapshot dùng synchronization vì được chia sẻ giữa nhiều client.
Transport chỉ khóa ngắn lúc emit/complete outbound sink để hỗ trợ nhiều sender đồng thời.

Factory, registry và Micrometer đều dùng bean `webSocketObservabilityClock`.
Có thể override bean này bằng Clock của ứng dụng/test. Event lưu Instant; statistics-zone
chỉ xác định ngày thống kê. Standalone factory nhận Clock qua constructor, không hard-code UTC.

Metrics gồm connection attempts, connect duration, state duration, retries, disconnects,
connection losses, heartbeat timeouts, errors, messages/bytes và dispatcher queue/processed/dropped/errors.
Gauge state: STOPPED=0, CONNECTING=1, CONNECTED=2, RETRY_WAIT=3, DISCONNECTING=4, FAILED=5.
Counter errors ghi một lần cho mỗi lỗi; heartbeat timeout có thêm counter chuyên biệt.
Tên connection/dispatcher/category phải hữu hạn. Một factory chỉ cho một client sống trên mỗi ConnectionId.
Snapshot và recent events là process-local, không thay backend metrics hay persistent storage.

## Thay đổi API

- ManagedWebSocketClient → WebSocketClient; WebSocketClientManagerFactory → WebSocketClientFactory.
- RetryPolicy → ReconnectPolicy; builder retryPolicy → reconnectPolicy.
- Heartbeat interval/pong timeout gộp vào HeartbeatOptions.
- Bỏ markReady/markAuthenticating/markResubscribing, readinessMode và lifecycleStageTimeout khỏi lib.
- TRANSPORT_CONNECTED/READY → CONNECTED; bỏ readyAt, readyTransitions, transportToReadyMillis.
- Overview dùng connectedConnections; readiness nghiệp vụ xem tại adapter/subscription.
- ConnectionObserver → WebSocketObserver; MicrometerConnectionObserver → MicrometerWebSocketObserver.
- Bỏ dispatcher BLOCK, scheduler pool-size và custom condition classes.
- Remote close 1000/1001 mặc định cũng reconnect nếu policy cho phép.

## Kiểm tra

```bash
./mvnw -f hit-core-lib/pom.xml -pl websocket-client-starter -am clean verify
```

Test dùng fake transport cho lifecycle và server loopback thật cho native Ping/Pong, HTTP handshake,
concurrent send, cancel-connect, remote close và ownership của pooled buffer.
