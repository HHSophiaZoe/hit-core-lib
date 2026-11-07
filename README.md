# HIT Core Lib

Maven library tập hợp các Spring Boot starter dùng chung cho các service HIT: web/security,
REST API, JPA, cache, Kafka, storage, chatbot và websocket.

Java 25, Spring Boot 4.1, Jackson 3. Các starter tự cấu hình bằng Spring Boot auto-configuration; ứng dụng chỉ cần
`@SpringBootApplication`, không scan package `com.hit`.

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

Cần JDK 25 và Maven 3.9.16 theo yêu cầu trong `pom.xml`. Chạy từ thư mục gốc của thư viện:

```bash
mvn clean install
```

Kiểm tra một starter cùng các dependency:

```bash
mvn -pl <starter> -am test
```

## Tài liệu

- [Chi tiết các starter](docs/starters.md)
