---
layout: post
title:  "Kafka Batch 처리 & DB 최적화"
date:   2025-06-19 22:50:00 +0900
categories: dev
---

# 들어가면서 

실무를 진행하면서 서버 간의 빈번한 요청을 Kafka를 이용해서 묶어서 요청을 보내도록 적용할 일이 생겼다. 
기존에 비슷한 샘플이 있어서, 그 부분을 참고해서 샘플 예시를 만들어 본다. 

# Kafka 배치 처리 핵심 정리

| 구분                 | 개념 & 설정 핵심                                                                                                                                                                                                                                         | 실습·운영 팁                                                                                            |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Batch Listener** | *Consumer*가 `poll()` 시 가져온 \*\*레코드 목록(List)\*\*을 한 번에 애플리케이션으로 전달.<br>Spring-Kafka는<br>`factory.setBatchListener(true)` + `@KafkaListener` 메서드 파라미터를 `List<ConsumerRecord<…>>` 로 선언하면 끝.                                                           | 단순 루프·bulk SQL·벡터 계산처럼 **여러 건을 한 번에 처리**할 때 유리.                                                    |
| **Batch 크기**       | `max.poll.records` *(default 500)*<br>→ poll 한 번에 받을 **최대 레코드 수**.<br>`fetch.min.bytes` & `fetch.max.wait.ms` 로 “메시지를 조금 모아서 보내 달라”는 힌트를 브로커에 줄 수 있음.                                                                                              | • 한 배치가 너무 크면 메모리·DB lock 길어짐.<br>• 너무 작으면 poll/commit 왕복이 늘어 TPS 낭비.                              |
| **Ack 전략**         | *Spring-Kafka* 컨테이너 속성<br>`AckMode`<br>‣ **BATCH** (기본) : 리스너 리턴 시 배치 전체 커밋<br>‣ **MANUAL(\_IMMEDIATE)** : 애플리케이션이 `Acknowledgment.acknowledge()` 호출해야 커밋                                                                                          | MANUAL 모드 + 재시도/에러핸들러 조합하면 **exactly-once like** 플로우 설계 가능                                         |
| **순차 vs 병렬**       | - Partition 단위 순서는 Kafka가 보장<br>- 하나의 Consumer Thread(concurrency = 1)가 여러 Partition을 순차 처리하면 **TPS 상한 = 단일 스레드 처리량**<br>- TPS 확장 = `partition 수 ↑` + `concurrency 수 ↑`                                                                            | CPU-bound 작업은 코어 수만큼 concurrency 늘리면 선형 확장. DB-bound 는 Batch insert·커넥션 풀 크기도 같이 고려                |

## docker-compose.yml

``` yml
version: "3.8"

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.1
    ports: ["2181:2181"]
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.6.1
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

## application.yml

``` yml

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: test-group
      properties:
        max.poll.records: 500
      auto-offset-reset: earliest
    listener:
      type: batch
      concurrency: 1

```

## topic 초기화 shell

``` shell
docker compose exec kafka kafka-topics \
  --delete --topic test-topic \
  --bootstrap-server localhost:9092

docker compose exec kafka kafka-topics \
  --create --topic test-topic \
  --partitions 16 --replication-factor 1 \
  --bootstrap-server localhost:9092
```


## KafkaConfig.java
concurrency를 1로 설정해서 순차적으로 polling 하도록 함

``` java

@EnableKafka
@Configuration
public class KafkaConfig {

    private static final String BOOTSTRAP = "localhost:9092";

    /* ---------- Producer ---------- */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class,
                org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /* ---------- Consumer ---------- */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500"
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(1);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}

```


# Case 1. 16개의 파티션에 각각 100건의 데이터가 있고, 이를 concurrency 1로 처리하는 방식

max poll records 갯수를 500건으로 설정했으므로, 1600건의 데이터 중에서 최대 500건씩 나눠서 polling 하게된다.

> **DemoConsumer**

``` java

@Component
public class DemoConsumer {

    @KafkaListener(topics = "${demo.topic:test-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {

        System.out.printf("📥 polled %d records%n", records.size());

        Map<Integer, Long> byPartition = records.stream()
                .collect(Collectors.groupingBy(ConsumerRecord::partition, Collectors.counting()));
        byPartition.forEach((p, c) -> System.out.printf("  • partition %d → %d%n", p, c));

        ack.acknowledge();
    }
}

```

> **DemoProducer**

``` java

@Component
public class DemoProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${demo.topic:test-topic}")
    private String topic;

    public DemoProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void sendMessages() {
        for (int p = 0; p < 16; p++) {
            for (int i = 0; i < 100; i++) {
                String value = "msg-part" + p + "-num" + i;
                kafkaTemplate.send(topic, p, null, value);
            }
        }
        System.out.println("✅ 1600 건 전송 완료");
    }
}

```

**테스트 결과**

```
2025-06-19T23:16:57.740+09:00  INFO 9250 --- [ad | producer-1] o.a.k.c.p.internals.TransactionManager   : [Producer clientId=producer-1] ProducerId set to 3 with epoch 0
✅ 1600 건 전송 완료

...

2025-06-19T23:17:01.351+09:00  INFO 9250 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : test-group: partitions assigned: [test-topic-0, test-topic-1, test-topic-2, test-topic-3, test-topic-4, test-topic-5, test-topic-6, test-topic-7, test-topic-8, test-topic-9, test-topic-10, test-topic-11, test-topic-12, test-topic-13, test-topic-14, test-topic-15]
📥 polled 500 records
  • partition 10 → 100
  • partition 12 → 100
  • partition 13 → 100
  • partition 14 → 100
  • partition 15 → 100
📥 polled 500 records
  • partition 6 → 100
  • partition 7 → 100
  • partition 8 → 100
  • partition 9 → 100
  • partition 11 → 100
📥 polled 500 records
  • partition 0 → 100
  • partition 2 → 100
  • partition 3 → 100
  • partition 4 → 100
  • partition 5 → 100
📥 polled 100 records
  • partition 1 → 100


```


# Case 2. 지속적으로 1~1000 사이의 데이터 쌓일때 concurrency 1로 처리하는 케이스

``` java

package dev.rumble.kafkatest.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    public RandomProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    @Value("${demo.topic:test-topic}")
    private String topic;

    @Scheduled(fixedDelay = 500)
    public void randomSend() {
        int msgs = 1 + random.nextInt(1_000);  // [1, 1 000]
        int partCnt = 16;

        for (int i = 0; i < msgs; i++) {
            int p = i % partCnt;               // round-robin 파티션
            kafkaTemplate.send(topic, p, null, "rnd-" + System.nanoTime());
        }
        System.out.println("▶️  produced {} records" + msgs);
    }

}


```

**테스트 결과**

``` 
📥 polled 500 records
  • partition 2 → 127
  • partition 4 → 5
  • partition 5 → 368
📥 polled 500 records
  • partition 2 → 241
  • partition 3 → 259
📥 polled 500 records
  • partition 0 → 368
  • partition 1 → 23
  • partition 3 → 109
📥 polled 345 records
  • partition 1 → 345
▶️  produced {} records305
📥 polled 3 records
  • partition 14 → 3
📥 polled 302 records
  • partition 0 → 20
  • partition 1 → 19
  • partition 2 → 19
  • partition 3 → 19
  • partition 4 → 19
  • partition 5 → 19
  • partition 6 → 19
  • partition 7 → 19
  • partition 8 → 19
  • partition 9 → 19
  • partition 10 → 19
  • partition 11 → 19
  • partition 12 → 19
  • partition 13 → 19
  • partition 14 → 16
  • partition 15 → 19
```

AWS의 Firehose 처럼 특정 시간 혹은 polling할 데이터 사이즈 크기에 따라서 아래와 같은 세팅도 가능하다.

``` java

@Component
public class DemoConsumer {

    private static final int THRESHOLD = 500;            // 건수 조건
    private static final long MAX_INTERVAL_MS = 1_000;   // 시간 조건

    private final List<ConsumerRecord<String, String>> buffer = new ArrayList<>();
    private long lastFlush = System.currentTimeMillis();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @KafkaListener(
            topics = "${demo.topic:test-topic}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, String>> batch,
                       Acknowledgment ack) {

        buffer.addAll(batch);

        long now = System.currentTimeMillis();
        boolean readyBySize  = buffer.size() >= THRESHOLD;
        boolean readyByTime  = now - lastFlush >= MAX_INTERVAL_MS;

        if (readyBySize || readyByTime) {
            flushBuffer(now);
            ack.acknowledge();     // 한꺼번에 커밋
        }
    }

    private void flushBuffer(long now) {
        log.info("🔥 flush {} records ({} ms since last)",
                buffer.size(), now - lastFlush);

        buffer.stream()
                .collect(Collectors.groupingBy(ConsumerRecord::partition,
                        Collectors.counting()))
                .forEach((p, c) -> log.info("  • partition {} → {}", p, c));

        buffer.clear();
        lastFlush = now;
    }
}



```

**테스트 결과** 

아래와 같이 500건이 polling 가능하면 polling시키고, 1초가 지나면 남아있는 숫자를 polling한다.

``` 
2025-06-19T23:38:04.825+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     : 🔥 flush 500 records (5 ms since last)
2025-06-19T23:38:04.825+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 0 → 170
2025-06-19T23:38:04.825+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 1 → 9
2025-06-19T23:38:04.825+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 2 → 153
2025-06-19T23:38:04.825+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 3 → 168
▶️  produced {} records116
▶️  produced {} records57
▶️  produced {} records164
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     : 🔥 flush 379 records (1409 ms since last)
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 0 → 15
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 1 → 175
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 2 → 14
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 3 → 14
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 4 → 15
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 5 → 15
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 6 → 14
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 7 → 14
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 8 → 14
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 9 → 13
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 10 → 13
2025-06-19T23:38:06.234+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 11 → 13
2025-06-19T23:38:06.235+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 12 → 13
2025-06-19T23:38:06.235+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 13 → 13
2025-06-19T23:38:06.235+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 14 → 12
2025-06-19T23:38:06.235+09:00  INFO 12034 --- [ntainer#0-0-C-1] d.r.kafkatest.component.DemoConsumer     :   • partition 15 → 12
▶️  produced {} records227

```

# DB 최적화

## SQL Query 작성 시 Subquery와 Join의 차이점

subquery는 조건을 확인할때 적합 (필터링만 하고 하나의 테이블에서만 데이터를 가져와도 될땐 Subquery)
join은 복잡한 Projection을 할때 적합

## Nested Loops Join의 경우 O(N^2)로 성능이 좋지 않음
=> 이를 해결하기 위해 Hash Join을 사용 (MySQL 5.7부터 지원)
Hash Join은 두 테이블을 해시 테이블로 만들어서 조인하는 방식으로, O(N) 성능을 보장

DB의 통계를 이용해서 데이터 분포나 크기를 파악하고, 이를 기반으로 최적의 조인 순서를 결정하는 것이 중요

중첩 루프는 가장 간단한 조인 알고리즘으로, 두 테이블의 모든 조합을 비교하여 일치하는 행을 찾는 방식

## Hash Join 
해시 조인은 두단계로 이루어짐
1. 해시맵, 해시 테이블을 생성한다. (버킷 기반으로 작동) (실행계획 시 Hash Semi Join 이런 식으로 나옴)
    -> 해시값을 이용해서 검증 
    -> 비교적 요소가 적은 것을 이용해서 맵을 생성 (메모리 양을 줄일 수 있음)
2. iterate하면서 해시맵에 있는 값을 찾는다.
    -> 해시맵에 있는 값과 비교해서 일치하는 값을 찾는다. (순차 루프)

-> 이상적으로 가장 작은 비용으로 처리한다.

## Megered Join
1. 테이블 정렬 (인덱스가 있을때 이 알고리즘을 선택하고, B+ 인덱스는 정렬된 구조이다.)
2. 두 테이블을 순회하면서 같은 컨디션인 것을 체크한다. (MySQL은 미지원)

Order By가 있으면 같이 사용
O(nLog(N))의 성능을 보장
