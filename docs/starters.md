# Hit Core Lib - Các starter

Java 25, Spring Boot 4.1, Jackson 3.

## Cách dùng

- Ứng dụng kế thừa `com.hit:hit-core-lib` làm parent (hoặc parent kế thừa nó) để dùng chung version và cấu hình build.
  Parent chỉ quản lý version (`dependencyManagement`) và plugin; mỗi module tự khai báo dependency nó dùng.
- Thêm starter cần dùng, không cần ghi version:

  ```xml
  <dependency>
      <groupId>com.hit</groupId>
      <artifactId>spring-starter</artifactId>
  </dependency>
  ```

- Các starter tự cấu hình qua `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  Ứng dụng dùng `@SpringBootApplication` bình thường, **không** scan package `com.hit`.
- Mọi bean của starter có `@ConditionalOnMissingBean` (trừ hạ tầng như filter/aspect), nên ứng dụng khai báo bean
  cùng kiểu để thay thế.
- Logging là log4j2 (do `spring-starter` mang vào). Enforcer cấm logback, `spring-boot-starter-logging`, Jackson 2
  khai báo trực tiếp và các thư viện cũ đã thay thế.

---

## core-common-starter

Model và util dùng chung, không phụ thuộc Spring Boot runtime (chỉ `spring-web` cho `HttpStatus`).

- Model: `PageableReqModel`, `PageableSearchReqModel`, `PageResModel`, `Filter`, `Search`, `Order`, `ResponseStatus`,
  `SimpleSecurityUser`...
- Mapper: `DomainMapper`, `ResponseMapper` (MapStruct).
- Util: `StringUtils`, `NumberUtils`, `TimeUtils`, `VndMoneyUtils`, `ThreadUtils`, `CollectionUtils`, `FileUtils`,
  `ReflectUtils`, `TraceUtils`, `PageUtils`.

## spring-starter

Starter cho web application.

| Auto-configuration | Nội dung | Bật khi |
|---|---|---|
| `HitSpringAutoConfiguration` | `JsonMapper`, `EventPublisher`, `DataConverter`, `TokenService` (mặc định JWT), message bundle (`messages` của app trước, `hit-messages` của starter làm dự phòng), `Translator`, aspect `@LogTracking`/`@RolesAllowed` | luôn |
| `HitWebMvcAutoConfiguration` | `GlobalExceptionHandler`, locale theo `Accept-Language` (mặc định `vi-VN`), resolver `@UserPrincipalRequest` / `@PaginationParameter`, CORS, filter trace id / log request / xác thực / phân quyền, OpenAPI, executor virtual thread cho Tomcat | servlet web app |
| `HitTaskExecutionAutoConfiguration` | `appTaskExecutor` (virtual thread hoặc pool theo `spring.threads.virtual.enabled`), `@EnableAsync`, `ExecutorManager`, truyền trace id vào task định kỳ | `app.task.executor.enable=true` (executor) |
| `HitHttpClientAutoConfiguration` | `defaultRestClient` (RestClient trên Apache HttpClient 5 có pool), `HttpService` | `http-client.default.enable=true` |
| `HitExcelAutoConfiguration` | `IExcelService` (Apache POI) | có POI |

Filter (theo thứ tự): CORS (0) → trace id (1) → log request (2, `app.enable-log-request-http`) → xác thực
(3, `app.security.filter.authentication`) → phân quyền (4, `app.security.filter.authorization`).
Đường dẫn public = mặc định (`/swagger-ui`, `/springdoc`, `/v3/api-docs`, `/actuator/health`, `/auth/login`) cộng
`app.security.api-whitelist`, so khớp theo **prefix**.

Lỗi trả về dạng `GeneralResponse`: validate body/param, UUID sai, JSON hỏng → 400 `VALIDATION_ERROR`;
`BaseResponseException` → status theo mã; lỗi khác → 500 `GENERAL_SERVER_ERROR`. `@RestControllerAdvice` của app
được ưu tiên hơn handler của starter.

Filter xác thực chỉ phụ thuộc interface `TokenService` (`com.hit.spring.security.token`). Bản mặc định
`JwtTokenService` ký HS256, khoá `app.security.jwt.secret-key` phải dài ít nhất 32 byte; chỉ tạo khi có jjwt. Muốn
dùng loại token khác thì khai báo bean `TokenService` của ứng dụng.

`ExecutorManager`: `zipTasks` / `zipTasksReturn` chạy song song và chờ tối đa `app.task.executor.task-timeout-seconds`
cho cả lời gọi. Khi ném lỗi (timeout, task lỗi, bị interrupt, executor từ chối) thì các task còn chạy bị interrupt.
`zipTasksReturn` dừng ngay ở task lỗi đầu tiên. Lỗi của task là `cause` của `ExecutorException`.

## jpa-starter

`HitJpaAutoConfiguration`: bind `datasource.default.*` (`DataSourceDefaultProperties`, kế thừa `HikariConfig`) cho
DataSource/EntityManager do ứng dụng tự khai báo, và `TransactionSupport`.

- `BaseJPARepository` = `JpaRepository` + `JpaSpecificationExecutor` + `QuerydslPredicateExecutor` (OpenFeign QueryDSL 7).
- `BaseJPAAdapter` / `BaseRepository`: CRUD theo model, search phân trang với filter động.
- `SqlUtils`, `SqlPredicateUtils`, `SqlDslPredicateUtils`, `ChunkUtils`, `PageableSearchFieldMapper`.

## api-starter

`BaseController`, `BaseService`, `IService` cho CRUD REST trên `jpa-starter`.

## cache-starter

`HitCacheAutoConfiguration` (chạy trước auto-configuration cache/Redis của Boot):

- Internal (`cache.internal.enable=true`): `internalCacheManager` Caffeine hoặc Ehcache 3 (JCache).
- External (`cache.external.enable=true`): Redisson 4 (`cache.external.redisson.*`), `externalCacheManager`,
  `StringRedisTemplate`, serializer Jackson 3, `RedisDistribution` và aspect `@DistributedLock`.
- `RedisCacheStoreImpl` / `InternalCacheStoreImpl`: lớp cơ sở cho store của ứng dụng.

## kafka-starter

Mỗi cụm Kafka là một khối `messaging.kafka.clusters.<tên>.*`, với **các key của `spring.kafka.*`** mà starter xử lý
(kết nối, `ssl`, `security`, `producer`, `consumer`, `listener`, `admin`, `template`) cộng các field riêng cho config
dễ gây lỗi nếu không để ý (chỉ producer và SASL; consumer dùng nguyên key của Spring Boot, config khác viết qua
`properties[...]`). Thêm cụm = thêm khối, không viết code.

```yaml
messaging:
  kafka:
    clusters:
      main:
        bootstrap-servers: kafka-main:9092
        producer:
          delivery-timeout: 20s
          request-timeout: 10s
        consumer:
          group-id: trading
          max-poll-interval: 40s
      market:
        bootstrap-servers: kafka-market:9092
        listener:
          ack-mode: batch
```

| Phần | Field riêng (ngoài key của Spring Boot) | Key Kafka |
|---|---|---|
| Cụm | `sasl.mechanism`, `sasl.jaas-config` | `sasl.mechanism`, `sasl.jaas.config` |
| Producer | `delivery-timeout`, `request-timeout`, `linger`, `max-block` | `*.ms` tương ứng |
| | `enable-idempotence`, `max-in-flight-requests`, `max-request-size` | `enable.idempotence`, `max.in.flight.requests.per.connection`, `max.request.size` |

- Mặc định giống Spring Boot: field để trống thì không gửi, Kafka dùng mặc định của nó (ghi trong Javadoc từng field
  của `KafkaClusterProperties`). Thời gian viết `20s` hoặc số trần là mili-giây.
- Ưu tiên từ thấp đến cao: `properties[...]` của cụm, field có kiểu, `producer/consumer.properties[...]`.
- Mỗi cụm độc lập, không thừa hưởng cấu hình của cụm khác.

### Bean và cách dùng

Mỗi cụm `<tên>` có `<tên>KafkaTemplate`, `<tên>KafkaListenerContainerFactory`, `<tên>KafkaAdmin`,
`<tên>KafkaProducerFactory`, `<tên>KafkaConsumerFactory`.

- **Một cụm**: các bean này cũng mang tên mặc định (`kafkaTemplate`, `kafkaListenerContainerFactory`...), dùng như
  Spring Boot bình thường, bean `NewTopic` được tạo trên cụm đó.
- **Từ hai cụm**: không có cụm mặc định, phải chỉ rõ cụm, thiếu thì app báo lỗi lúc khởi động:

  ```java
  @Qualifier("marketKafkaTemplate") KafkaTemplate<String, String> marketTemplate;
  @KafkaListener(topics = "ticks", containerFactory = "marketKafkaListenerContainerFactory")
  @KafkaListener(topics = "ticks", batch = "true", containerFactory = "marketKafkaListenerContainerFactory")
  marketKafkaAdmin.createOrModifyTopics(new NewTopic("ticks", 6, (short) 3));   // không dùng bean NewTopic
  ```

- Có `clusters` thì auto-configuration Kafka của Spring Boot bị bỏ (`KafkaClustersImportFilter`), để Boot không tạo
  bean trên `spring.kafka.*` (`localhost:9092`). Không có `clusters` thì Spring Boot cấu hình Kafka từ
  `spring.kafka.*` như bình thường.
- Bean của ứng dụng áp dụng cho mọi cụm, như Spring Boot làm cho `spring.kafka`:
  `DefaultKafkaProducerFactoryCustomizer` / `DefaultKafkaConsumerFactoryCustomizer` (metrics Micrometer đi qua đây),
  `CommonErrorHandler`, `RecordInterceptor`, `BatchInterceptor`, `RecordMessageConverter`, `BatchMessageConverter`,
  `RecordFilterStrategy`, `AfterRollbackProcessor`, `ConsumerAwareRebalanceListener`, `ContainerCustomizer`,
  `ProducerListener`. Listener chạy virtual thread khi `spring.threads.virtual.enabled=true`.
- `producer.transaction-id-prefix` bật transaction cho producer và listener container của cụm đó; không tạo bean
  transaction manager để không đè `transactionManager` của JPA.
- `KafkaPublisher` là lớp để kế thừa, gửi JSON qua `KafkaTemplate<String, String>` truyền vào constructor.

## storage-starter

`BaseStorageCommand` với các bản Local, FTP (Spring Integration) và MinIO (MinIO client 9). Tạo trực tiếp từ
`StorageFileConfig`, không phải bean.

## chatbot-starter

`HitChatBotAutoConfiguration`:

- Telegram (`chatbot.telegram.enable=true`): `TelegramApiClient` (JDK HttpClient + Jackson 3), long polling
  `TelegramUpdatePoller`, `TelegramChatBotServiceImpl` (mặc định khi bật cả hai bot).
- Discord (`chatbot.discord.enable=true`): JDA 6 kết nối nền (app không chờ Discord khi khởi động; lần gửi đầu chờ tối
  đa `chatbot.discord.ready-timeout`, mặc định 30s), `DiscordChatBotServiceImpl`.
- Method có `@ChatBotMessageListener` trên bất kỳ bean nào (kể cả khai báo ở class cha / interface, bean có proxy)
  được đăng ký vào `ChatBotMessageDispatcher`.

Gửi tin (`ChatBotService`):

| | Telegram | Discord |
|---|---|---|
| `sendMessage(chatId, content)` | text thường | markdown của Discord |
| `sendMessage(MessageRequest)` | **HTML**: title in đậm + gạch, content là HTML (tag Telegram hỗ trợ), text động không cần escape (`TelegramHtmlUtils.sanitize`) | embed: title, màu theo type, description markdown, field/file của `DiscordMessageRequest` |
| Tin dài | tách thành nhiều tin ≤ 4096 ký tự, cắt ở xuống dòng, tag HTML đang mở được đóng/mở lại | tách ≤ 2000 ký tự (embed ≤ 4096), khối code ``` được giữ |
| Giới hạn tốc độ | 429: chờ `retry_after` (tối đa `chatbot.telegram.max-retry-wait`, mặc định 30s) rồi gửi lại một lần | JDA tự xếp hàng |
| `sendMessageAsync` | chạy trên `TaskExecutor` | chạy trên `TaskExecutor` |
| `sendPrivateMessage(userId, …)` | gửi tới chat của user (user phải từng nhắn bot) | mở kênh riêng qua API |
| Lỗi | ghi log, không ném exception | ghi log, không ném exception |

Telegram chỉ dùng HTML (không có Markdown: bản cũ Telegram đã ngừng phát triển, MarkdownV2 phải escape 18 ký tự).
Discord không hỗ trợ HTML.

## websocket-client-starter

Client WebSocket trên Reactor Netty: connection có reconnect, pool, dispatcher theo key, observability (in-memory và
Micrometer khi có `MeterRegistry`). Cấu hình `websocket-client.*`.

