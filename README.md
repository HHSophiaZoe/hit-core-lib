# HIT Core Lib

Maven library tập hợp các Spring Boot starter dùng chung cho các service HIT: web/security,
REST API, JPA, cache, Kafka, storage, chatbot, websocket và test utilities.

Đây là Git submodule độc lập. Thay đổi trong module phải được commit tại repository
`hit-core-lib`, sau đó cập nhật submodule pointer ở repository cha.

## Starter modules

- `spring-starter`
- `core-common-starter`
- `api-starter`
- `jpa-starter`
- `cache-starter`
- `kafka-starter`
- `storage-starter`
- `chatbot-starter`
- `websocket-starter`
- `test-core-lib`

## Build

```bash
mvn clean install
```

## Tài liệu

- [Chi tiết các starter](docs/STARTERS.md)
