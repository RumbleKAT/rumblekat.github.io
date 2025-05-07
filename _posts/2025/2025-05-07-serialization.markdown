---
layout: post
title:  "Serialization 관련"
date:   2025-05-07 21:21:00 +0900
categories: dev
---
## @Data를 사용하면서, Boolean 메서드명이 is로 시작하는 경우
 
 - private final을 사용하면, Setter를 제외하고 사용

``` java

@Data
public class SampleDomain {

    private final Long id;
    private final String name;
    private final Boolean hidden; 

    // 다른 객체에서 Sample Domain의 로직이 필요한 경우
    @JsonIgnore
    public Boolean isHiddenItem(){ 
        return name.equals("sample");
    } // --> jsonIgnore가 없으면 직렬화시 해당 로직이 계속 수행
}
```

### 적용 범위

| 위치                                                                     | 애너테이션 없음 (`@JsonIgnore` X) | `@JsonIgnore` 붙인 경우 |
| ---------------------------------------------------------------------- | -------------------------- | ------------------- |
| **`hidden` 필드** (및 Lombok가 만든 `getHidden()`)<br>→ JSON 속성 **`hidden`** | ✅ 포함                       | 🔒 빠짐               |
| **`isHiddenItem()` 메서드**<br>→ JSON 속성 **`hiddenItem`**                 | ✅ 포함                       | 🔒 빠짐               |

Jackson-databind는 **모든 “getter”**( `getXxx()` 또는 `isXxx()` )를 JavaBean 프로퍼티로 간주해 직렬화‧역직렬화를 시도한다.
따라서 아무 표시를 하지 않으면 **두 속성**이 모두 적용된다.

```json
// 애너테이션 없음
{
  "id": 1,
  "name": "sample",
  "hidden": true,
  "hiddenItem": true
}
```

---

### `@JsonIgnore`를 어디에 붙이느냐에 따른 차이

| 붙이는 위치                                                                | 결과                                       | 주 사용 목적                                       |
| --------------------------------------------------------------------- | ---------------------------------------- | --------------------------------------------- |
| **`private final Boolean hidden;`**<br>(또는 Lombok가 만든 `getHidden()`에) | `hidden` 속성이 제거된다. `hiddenItem`은 그대로 남음. | *저장용 플래그* 는 서버 안에서만 쓰고, 클라이언트에는 숨길 때.         |
| **`public Boolean isHiddenItem()`**                                   | `hiddenItem` 속성이 제거된다. `hidden`은 그대로 남음. | *계산된 파생값* 을 노출하지 않거나, 동일 내용을 두 번 보낼 필요가 없을 때. |
| **둘 다**                                                               | 두 속성 모두 제거 → `{ "id": …, "name": … }`    | 완전히 감추고 싶을 때.                                 |

> **역직렬화(요청 → 객체)** 도 동일하게 적용
> JSON 이 속성을 포함하고 있어도 `@JsonIgnore` 가 붙은 필드/메서드는 무시

---

## Kyro를 사용한 DTO Serialization

| 특징             | 설명                                                                                                                                              |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **매우 작은 바이너리** | reflection·필드 이름을 포함하지 않으므로 Java Serializable 대비 10 배 이상 작아지는 경우도 많음                                                                         |
| **고속**         | 필드 오프셋·ASM 바이트코드를 활용해 getter/setter 호출을 건너뜀 <br/>Afterburner 적용 Jackson JSON <  Kryo <  protobuf 처럼, “텍스트형보다는 빠르고 스키마 기반보다는 느릴 수 있음” 정도 성능. |
| **객체 그래프 보존**  | 동일 객체 중복 참조, 순환 참조 모두 지원.                                                                                                                       |
| **클래스 등록**     | 직렬화 대상 클래스를 **ID(정수)** 로 등록 → 바이트 수 절감·역직렬화 안전성 확보.                                                                                             |
| **스레드 안전 X**   | Kryo 인스턴스를 **ThreadLocal** 로 보관하거나 풀(pool)로 관리해야 함.                                                                                             |

> 의존성 추가

```
implementation 'com.esotericsoftware:kryo5:5.6.0'
```

> KyroUtil.java

``` java

public class KryoUtil {
    /* ───────── Kryo 풀(Thread-safe) ───────── */
    private static final int                       POOL_SIZE = 16;
    private static final ArrayBlockingQueue<Kryo> POOL      = new ArrayBlockingQueue<>(POOL_SIZE);

    static {
        for (int i = 0; i < POOL_SIZE; i++) POOL.add(create());
    }

    /** Kryo 인스턴스 하나 생성 */
    private static Kryo create() {
        Kryo kryo = new Kryo();

        /* ① 전역 기본 직렬화기를 TaggedFieldSerializer 로 지정 */
        kryo.setDefaultSerializer(TaggedFieldSerializer.class);

        /* ② DTO 등록(ID=10) — 클래스 이름을 쓰지 않아 바이트 절감 */
        kryo.register(ItemDto.class, 10);

        return kryo;
    }

    public static byte[] write(ItemDto dto) {
        Kryo kryo = POOL.poll();
        if (kryo == null) kryo = create();

        try (Output output = new Output(128, -1)) {  // 128B부터 자동 확장
            kryo.writeObject(output, dto);
            return output.toBytes();
        } finally {
            POOL.offer(kryo);
        }
    }

    public static ItemDto read(byte[] bytes) {
        Kryo kryo = POOL.poll();
        if (kryo == null) kryo = create();

        try (Input input = new Input(bytes)) {
            return kryo.readObject(input, ItemDto.class);
        } finally {
            POOL.offer(kryo);
        }
    }
}


```

> ItemDto

``` java

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {

    private static final int UID = 13;

    @TaggedFieldSerializer.Tag(1)
    private Long id;

    @TaggedFieldSerializer.Tag(2)
    private String name;

    @TaggedFieldSerializer.Tag(3)
    private Boolean hidden;
}

```

> biz.java

``` java

ItemDto original = new ItemDto(1L, "sample", true);

byte [] serialized = write(original);
System.out.printf("바이트 길이: %d%n", serialized.length);

ItemDto restored = read(serialized);
System.out.println("역직렬화 결과: " + restored);

```

> **결과**

```
바이트 길이: 14
역직렬화 결과: ItemDto(id=1, name=sample, hidden=true)
```
