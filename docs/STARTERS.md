# Hit Core Lib - Chi Tiết Các Modules

## 1. spring-starter

**Mục đích**: Core Spring Boot configurations

**Dependencies**:
- spring-boot-starter-web
- spring-boot-starter-aop
- spring-boot-starter-actuator
- spring-boot-starter-validation
- spring-security-crypto
- JWT (jsonwebtoken)
- Apache HttpClient5, OkHttp
- Apache POI (Excel)
- Google Guava

**Cung cấp**:
- Web configurations
- Security utilities
- REST client helpers
- File processing utilities
- Validation support

---

## 2. core-common-starter

**Mục đích**: Common models và utilities

**Cung cấp**:
- `PageResModel<T>`: Pagination response
- `PageableReqModel`: Pagination request
- `PageableSearchReqModel`: Search with pagination
- Common utilities
- Shared constants

---

## 3. api-starter

**Mục đích**: Base classes cho REST API

**Cung cấp**:

### BaseService<M, E, ID, Repo, Map>

Generic service với CRUD operations:

```java
// M: Model (DTO)
// E: Entity
// ID: ID type
// Repo: Repository
// Map: Mapper

// Methods:
getById(ID id)                          // Get by ID
getBasicById(ID id)                     // Get basic info
getByIds(List<ID> ids)                  // Get multiple
select(PageableReqModel request)        // Pagination
search(PageableSearchReqModel request)  // Search with pagination
deleteById(ID id)                       // Delete by ID
deleteByIds(Set<ID> ids)                // Delete multiple
```

### BaseController

Base class cho REST controllers

### IService

Service interface

### ItemPermission

Permission model

---

## 4. jpa-starter

**Mục đích**: JPA/Hibernate + QueryDSL

**Dependencies**:
- spring-boot-starter-data-jpa
- QueryDSL JPA (Jakarta)

**Cung cấp**:

### BaseRepository<E, ID>

```java
// Extends: JpaRepository + QuerydslPredicateExecutor

// Methods:
E getOne(ID id)
List<E> getAllByIdIn(List<ID> ids)
List<ID> getAllId(Set<ID> ids)
PageResModel<E> search(PageableReqModel request)
PageResModel<E> search(PageableSearchReqModel request)
void delete(ID id)
void delete(Set<ID> ids)
```

**QueryDSL Support**:
- Type-safe queries
- Q-classes auto-generated
- BooleanBuilder for dynamic queries

---

## 5. cache-starter

**Mục đích**: Multi-level caching (Redis + EhCache)

**Dependencies**:
- Redisson 3.35.0

**Cung cấp**:

### Configurations
- `CacheStarterConfig`: Main config
- `RedisCacheConfig`: Redis setup
- `InternalCacheConfig`: EhCache setup
- `CacheConfigProperties`: Properties binding

### Conditional Annotations
- `@ConditionalOnExternalCacheEnable`: Enable Redis
- `@ConditionalOnInternalCacheEnable`: Enable EhCache

### Cache Stores
- `BaseExternalCacheStore`: Redis operations
- `RedisCacheStoreImpl`: Redis implementation
- `BaseInternalCacheStore`: Internal cache operations
- `InternalCacheStoreImpl`: EhCache implementation

### Distributed Locking
- `@DistributedLock`: Annotation for distributed locks
- `DistributedLockAbstractProcessor`: Lock processor
- `DistributedLockSimpleLockProcessor`: Simple lock

### Helpers
- `CacheContext`: Cache context management
- `DistributedAtomic`: Atomic operations
- `RedisDistribution`: Distributed utilities
- `ExpressionEvaluator`: SpEL evaluation

### Lua Scripts
- `deleteByKeyValue.lua`: Atomic delete
- `setIfAbsent.lua`: Set if not exists
- `setIfAbsentWithSuffix.lua`: Set with suffix

---

## 6. storage-starter

**Mục đích**: File storage integration

**Dependencies**:
- Cloudinary HTTP44

**Cung cấp**:
- Cloudinary integration
- File upload/download utilities
- Image processing

---

## 7. chatbot-starter

**Mục đích**: Chatbot integration

**Cung cấp**:
- `ChatBotAnnotationScanner`: Scan chatbot annotations
- Chatbot utilities

---

## 8. kafka-starter

**Mục đích**: Kafka messaging

**Cung cấp**:
- Kafka producer/consumer configurations
- Message serialization
- Error handling

---

## 9. test-core-lib

**Mục đích**: Testing utilities

**Cung cấp**:
- Test base classes
- Test utilities
- Mock helpers

---

## Cách Sử Dụng

### 1. Thêm Dependency

```xml
<dependency>
    <groupId>com.hit</groupId>
    <artifactId>spring-starter</artifactId>
    <version>${spring-starter.version}</version>
</dependency>
```

### 2. Extend Base Classes

```java
@Service
public class UserService extends BaseService<
    UserModel, UserEntity, Long, UserRepository, UserMapper
> {
    // Custom business logic
}
```

### 3. Configure Properties

```yaml
cache:
  external:
    enabled: true
  internal:
    enabled: true
```

---

## Version Management

Tất cả versions được quản lý trong parent POM:

```xml
<properties>
    <spring-starter.version>1.0</spring-starter.version>
    <core-common-starter.version>1.0</core-common-starter.version>
    <api-starter.version>1.0</api-starter.version>
    <jpa-starter.version>1.0</jpa-starter.version>
    <cache-starter.version>1.0</cache-starter.version>
    <storage-starter.version>1.0</storage-starter.version>
    <chatbot-starter.version>1.0</chatbot-starter.version>
    <kafka-starter.version>1.0</kafka-starter.version>
</properties>
```

---

## Build Commands

```bash
# Build all
mvn clean install

# Build specific starter
cd hit-core-lib/spring-starter
mvn clean install

# Build service using starters
mvn clean package -pl hit-trading-stock-service -am
```
