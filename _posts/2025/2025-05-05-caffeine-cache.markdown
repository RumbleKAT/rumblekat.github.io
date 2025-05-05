---
layout: post
title:  "Caffeine Cache And Immutable Object"
date:   2025-05-05 17:25:00 +0900
categories: dev
---

# Caffeine Cache란?

### 1. Caffeine Cache란? 

> **Caffeine** 은 Ben Manes가 만든 **고성능 JVM 로컬(in-process) 캐시 라이브러리**입니다.
> Guava `Cache`의 후속 격으로, 최신 논문들을 바탕으로 설계된 **W-TinyLFU** (admission) + **LRU/LFU 하이브리드** (eviction) 정책이 핵심입니다.

```java
// 단독 사용 예 – 10 분 TTL, 최대 10 만 건, 기록용 CacheStats ON
LoadingCache<Long, User> userCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .recordStats()
        .build(id -> userRepository.findById(id).orElseThrow());
```

* **O(1)** 접근 · 우수한 hit/miss 예측 정확도
* **JDK 8+ only** – 순수 Java, 의존성 하나
* **Spring Cache**: `spring-boot-starter-cache` + `spring-boot-starter-caffeine` 로 즉시 사용 가능

---

### 2. 다른 대표적 로컬 캐시(EhCache 3 등)와 무엇이 다른가?

| 항목                  | **Caffeine**                                          | **EhCache 3**                                           |
| ------------------- | ----------------------------------------------------- | ------------------------------------------------------- |
| **핵심 목표**           | **최고의 in-memory 성능** (µs 단위)                          | **기능 풍부**(TTL·TTI·디스크·오프힙·클러스터)                         |
| **주요 정책**           | **W-TinyLFU** (아주 낮은 오버헤드 + hit-ratio ↑)              | LRU·LFU·FIFO *(정책별 선택)*                                 |
| **저장 영역**           | **온-힙 전용** (GC-친화적 구조)                                | 온-힙·오프힙·디스크·Terracotta 클러스터                             |
| **동시성**             | **striped counter + lock-free list**<br>스레드당 CAS 충돌 ↓ | 세그먼트 락 + off-heap 멀티-슬랩                                 |
| **JSR-107(JCache)** | 별도 `caffeine-jcache` 어댑터 필요                           | **1급 지원** (API·SPI 완전 호환)                               |
| **Spring 통합**       | Boot 의존성 하나로 끝                                        | Boot 3.x 부터 `spring-boot-starter-cache` + `ehcache-xml` |
| **모니터링**            | `cache.stats()` → hitRatio, eviction 수                | Dropwizard·Micrometer·JMX                               |
| **지속성/재시작 내구성**     | X (메모리 소멸)                                            | 디스크·off-heap·클러스터 저장 O                                  |
| **주요 쓰임새**          | 고QPS 읽기 캐시, 조회-전용 데이터, 1-\~10 GB JVM                  | 대용량·장기 데이터, 장애 시 warm-restart, 분산 동기화                   |

> **요약** —
> *읽기 성능·간결함*이 관건이면 **Caffeine**,
> *영속성·여러 계층·JSR-107 완전 호환*이 필요하면 **EhCache**가 어울립니다.

---

### 3. Spring Boot에서의 간단 비교 설정

```yaml
# application.yml

spring:
  cache:
    type: caffeine     # or ehcache
    caffeine:
      spec: maximumSize=100000,expireAfterWrite=5m,recordStats
    # ehcache:
    #   config: classpath:ehcache.xml
```

* **Caffeine**: YAML 한 줄로 정책 지정 → 런타임에 `Caffeine.newBuilder().build()` 생성
* **EhCache**: XML (or Java) 구성으로 티어별 용량·계층 지정, 필요 시 디스크 경로/클러스터 주소 설정

---

# 2. Caffein Cache Example

dev.rumble.caffeinecache.config.CacheConfig.java
``` java

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public SimpleCacheManager cacheManager(){
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS) // 1초 동안 유지된다.
                .maximumSize(100); // 최대 100건까지 저장된다.

        CaffeineCache userCache = new CaffeineCache("userCache", caffeine.build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(userCache));
        return manager;
    }
}

```
dev.rumble.caffeinecache.domain.User.java
``` java
public record User(Long id, String name) {}
```

dev.rumblekat.caffeincache.service.UserService.java
``` java
@Service
public class UserService {

    private final AtomicInteger counter = new AtomicInteger();

    private final Map<Long, User> dummyDatabase = Map.of(
            1L, new User(1L, "Alice"),
            2L, new User(2L, "Bob")
    );

    @Cacheable(value = "userCache", key = "#id")
    public User getUserById(Long id) {
        counter.incrementAndGet();
        System.out.println("Fetching from DB for id: " + id);
        return dummyDatabase.get(id);
    }

    public int getAtomicCounter(){
        return this.counter.get(); // 데이터 갱신시에만 카운트가 추가된다.
    }
}
```
test code

``` java
    @Test
    void testCaffeineCacheWithTTL() throws InterruptedException{
        // 1. 캐시에 저장됨
        User result1 = userService.getUserById(1L);
        System.out.println("First call: " + result1);

        Thread.sleep(2000); // TTL은 config에 지정된 시간을 기준으로 한다.

        User result2 = userService.getUserById(1L);
        System.out.println("Second call: " + result2);

        assertEquals(result1, result2);

        int cacheHitCounts = userService.getAtomicCounter();
        assertEquals(cacheHitCounts, 2);
    }
```
---

# Immutable Collections

**개요**

| 특성                    | 설명                                                         |
| --------------------- | ---------------------------------------------------------- |
| **절대 불변**             | `add/remove/clear` 호출 시 즉시 `UnsupportedOperationException` |
| **스레드 안전(read-only)** | 내부 구조가 변하지 않아 동기화 없이 여러 스레드가 동시에 읽어도 안전                    |
| **null 금지**           | 어떤 위치에도 `null` 원소/키/값 불가 (검증 실패 시 NPE)                     |
| **예측 가능한 메모리/성능**     | 크기 확정 ⇒ 배열 기반 레이아웃 + 해시 분포 최적화                             |


**종류**

| 타입                              | 용도                                     | 예시 생성                                                                        |
| ------------------------------- | -------------------------------------- | ---------------------------------------------------------------------------- |
| **ImmutableList**               | 순서 중요, 중복 허용                           | `ImmutableList.of("A","B")`                                                  |
| **ImmutableSet**                | 순서 X, 중복 제거, 해시 기반                     | `ImmutableSet.copyOf(list)`                                                  |
| **ImmutableSortedSet**          | 정렬 유지, `Comparable` or `Comparator` 필요 | `ImmutableSortedSet.naturalOrder().addAll(list).build()`                     |
| **ImmutableMap**                | 키-값 일대일                                | `ImmutableMap.of(1,"one",2,"two")`                                           |
| **ImmutableBiMap**              | 키=값 모두 고유, 양방향 역조회 `inverse()`         | `ImmutableBiMap.of("OK",200,"NOT_FOUND",404)`                                |
| **ImmutableMultimap**           | **한 키-다중 값**                           | `ImmutableMultimap.builder().put("role","ADMIN").put("role","USER").build()` |
| **ImmutableMultiset**           | **중복 개수(count)** 를 갖는 Set              | `ImmutableMultiset.copyOf(List.of("A","A","B"))`                             |
| **ImmutableTable / RangeSet 등** | 2-차원 맵, 구간 집합 등 특수 컬렉션                 | `ImmutableTable.<R,C,V>builder() … build()`                                  |


**예시**
``` java
// ① 정적 팩터리
ImmutableList<String> a = ImmutableList.of("red", "green", "blue");

// ② Builder – 가독성 + 조건부 add
ImmutableSet<Integer> lotto = ImmutableSet.<Integer>builder()
        .add(3, 13, 23)
        .addAll(List.of(33, 43, 45))
        .build();

// ③ copyOf – 기존 컬렉션을 그대로 불변화
ImmutableMap<Long, User> users = ImmutableMap.copyOf(mutableMap);

// ④ Java Stream Collector (JDK 9+)
ImmutableSet<User> userSet = stream.collect(ImmutableSet.toImmutableSet());
```

**객체 비교 예시**

``` java
enum AccessLevel{
    GUEST, USER, ADMIN
}

@Getter
class AccountRole{
    private final String name;
    private final AccessLevel level;

    public AccountRole(String name, AccessLevel level) {
        this.name = name;
        this.level = level;
    }

    private String primaryKey(){
        return name + "|" + level.name();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.primaryKey());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof AccountRole other)) return false;
        return primaryKey().equals(other.primaryKey());
    }
}


@Test
void test_immutableSet(){
    ImmutableSet<Integer> lotto = ImmutableSet.<Integer>builder()
            .add(3,13,23)
            .addAll(List.of(33, 43, 45, 23))
            .build();
    assertTrue(lotto.contains(13));
    assertEquals(lotto.size(), 6);
}

@Test
void test_immutableSet_Enum(){
    ImmutableSet<AccountRole> roles = ImmutableSet.<AccountRole>builder()
            .add(new AccountRole("read-only", AccessLevel.GUEST))
            .add(new AccountRole("read-only", AccessLevel.USER))
            .add(new AccountRole("read-only", AccessLevel.USER))
            .add(new AccountRole("standard", AccessLevel.USER))
            .add(new AccountRole("super-user", AccessLevel.ADMIN))
            .build();

    assertEquals(4, roles.size()); // 키값이 같은 경우, 중복 제거가 된다.

}

@Test
void immutableMap_basic(){
    ImmutableMap<Integer, String> idToName = ImmutableMap.<Integer, String> builder()
            .put(1, "Alice")
            .put(2, "Bob")
            .put(3, "Charlie")
            .build();

    assertEquals("Bob", idToName.get(2));
    assertThrows(UnsupportedOperationException.class, () -> idToName.put(4,"Dave"));
}

@Test
void immutableMap_duplicateKey_keepLast(){
    List<Map.Entry<Integer, String>> raw = List.of(
            Map.entry(1, "A1"),
            Map.entry(1, "A2"),
            Map.entry(2, "B")
    );

    ImmutableMap<Integer, String> map =
            raw.stream()
                    .collect(ImmutableMap.toImmutableMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> newValue
                    )); // 같은 키값인 경우, 어떤 값을 우선시할지

    assertEquals(2, map.size());
    assertEquals("A2", map.get(1));

}

```


