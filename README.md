# HIT Core Lib

Maven library tập hợp các Spring Boot starter dùng chung cho các service HIT: web/security,
REST API, JPA, cache, Kafka, storage, chatbot và websocket.

Java 25, Spring Boot 4.1, Jackson 3. Các starter tự cấu hình bằng Spring Boot auto-configuration; ứng dụng chỉ cần
`@SpringBootApplication`, không scan package `com.hit`.

Đây là Git submodule độc lập. Thay đổi trong module phải được commit tại repository
`hit-core-lib`, sau đó cập nhật submodule pointer ở repository cha.

## Các starter

- `spring-starter`
- `core-common-starter`
- `api-starter`
- `jpa-starter`
- `cache-starter`
- `kafka-starter`
- `storage-starter`
- `chatbot-starter`
- `websocket-client-starter`

## Build và kiểm tra

Cần JDK 25. Từ root workspace `hit-trading-system` (nơi có Maven Wrapper):

```bash
./mvnw -f hit-core-lib/pom.xml clean install
```

Build một starter cùng các dependency bằng
`./mvnw -f hit-core-lib/pom.xml -pl <starter> -am test`.
Checkout riêng của lib không có Maven Wrapper; cần Maven tương thích với cấu hình POM.
Thư viện không chạy độc lập và không dùng env chung; ứng dụng sử dụng starter cung cấp cấu hình.

## Tài liệu

- [Chi tiết các starter](docs/starters.md)
