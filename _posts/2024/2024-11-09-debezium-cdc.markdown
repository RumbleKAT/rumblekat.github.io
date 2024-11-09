---
layout: post
title:  "Debezium-CDC"
date:   2024-11-09 21:58:00 +0900
categories: dev
---

# 참고자료
> https://github.com/irtiza07/postgres_debezium_cdc

# 1.Debezium이란 무엇인가?

Debezium은 오픈소스 데이터 변경 캡처(Change Data Capture, CDC) 플랫폼으로, 데이터베이스에서 발생하는 모든 변경 사항(삽입, 업데이트, 삭제 등)을 실시간으로 추적하여 이를 다른 시스템에 전달할 수 있습니다. Debezium은 Kafka를 기반으로 동작하며, Kafka 커넥터 형태로 제공되어 이벤트 기반의 데이터 스트리밍이 필요한 환경에서 많이 사용됩니다.

**Debezium의 주요 기능**:

1. **실시간 데이터 스트리밍**: 데이터베이스 내의 모든 변경 사항을 캡처하고 이를 실시간으로 스트리밍하여, 최신 데이터를 유지하면서 시스템 간 데이터 동기화가 가능하게 합니다.
2. **데이터 일관성 보장**: 트랜잭션 단위로 데이터를 전송하여 일관성을 보장하며, Kafka의 강력한 데이터 처리와 결합되어 장애 발생 시에도 데이터 손실 없이 복구가 용이합니다.
3. **복잡한 ETL 작업 간소화**: 데이터 변경 사항만을 스트리밍하여 대용량 데이터베이스에서도 효율적으로 데이터를 처리할 수 있습니다.

**Debezium의 대표적인 커넥터**:

- **MySQL 커넥터**: MySQL 데이터베이스의 변경 사항을 캡처하여 스트리밍할 수 있습니다.
- **PostgreSQL 커넥터**: PostgreSQL의 논리적 복제를 활용해 변경 사항을 캡처합니다.
- **MongoDB 커넥터**: MongoDB의 오퍼레이션 로그(OpLog)를 읽어 데이터 변경을 추적합니다.
- **SQL Server 커넥터**: SQL Server의 CDC 기능을 사용하여 변경 사항을 캡처합니다.
- **Oracle 커넥터**: Oracle 데이터베이스의 변경 사항을 캡처할 수 있는 커넥터입니다.

**기타 커넥터**:

Debezium 외에도 Kafka 커넥트 생태계에는 다양한 데이터 소스와 싱크를 지원하는 커넥터들이 있습니다. 예를 들어:

1. **Confluent 커넥터**: Confluent에서 제공하는 Kafka 커넥터로, Salesforce, Google BigQuery, ElasticSearch 등 다양한 외부 시스템과 통합할 수 있는 커넥터가 있습니다.
2. **JDBC 커넥터**: 일반적인 데이터베이스와 연결이 가능하며, JDBC 드라이버를 사용하는 데이터 소스와 연동할 수 있습니다.
3. **Elasticsearch 커넥터**: Kafka 스트림 데이터를 Elasticsearch로 전송하여 검색 기능에 활용할 수 있습니다.
4. **AWS S3 커넥터**: Kafka 데이터를 AWS S3로 전송하여 장기 저장 및 아카이빙에 사용합니다.
   
Debezium은 특히 **데이터베이스와 이벤트 기반 아키텍처 간의 실시간 데이터 동기화**에 적합하여, 실시간 분석, 모니터링, 마이크로서비스 아키텍처 등에 많이 활용됩니다.

# 2.Debezium은 kafka에서만 사용할 수 있는 것인가?

Debezium은 기본적으로 **Kafka를 통해 데이터 변경 이벤트를 전달**하도록 설계되었지만, Kafka 없이도 다른 메시징 시스템이나 데이터 처리 시스템과 연결할 수 있도록 여러 확장 옵션이 존재합니다.

**Kafka 없이 Debezium을 사용하는 방법**:

1. **Debezium Server**: 
   - Debezium은 Kafka 외에 다른 시스템으로 데이터를 스트리밍할 수 있도록 **Debezium Server**를 제공합니다.
   - Debezium Server는 기본적으로 Kafka 커넥터 프레임워크를 유지하되, **Kafka 대신 다른 메시지 브로커**나 데이터베이스로 CDC 이벤트를 전달할 수 있습니다.
   - 지원되는 출력 대상: **Amazon Kinesis**, **Google Pub/Sub**, **Apache Pulsar**, **Redis Streams** 등입니다. 이 옵션을 사용하면 Kafka 환경 없이도 다양한 시스템에 CDC 이벤트를 전송할 수 있습니다.

2. **Kafka Connect를 통한 HTTP Sink**:
   - Kafka를 사용하지 않는 경우에도 HTTP API를 활용해 특정 애플리케이션으로 CDC 이벤트를 보낼 수 있습니다.
   - 이 방법은 **Kafka Connect의 HTTP Sink 커넥터**를 사용하여 데이터 변화를 RESTful HTTP API로 전송할 수 있습니다.
   
3. **Custom Sink 개발**:
   - Debezium의 데이터를 다른 시스템으로 보내기 위해 **사용자 정의 싱크(Sink)** 애플리케이션을 개발할 수도 있습니다.
   - 예를 들어, Debezium이 CDC 이벤트를 Kafka에 전송하도록 설정한 후, Kafka 컨슈머 애플리케이션을 개발하여 데이터를 특정 애플리케이션이나 데이터베이스로 직접 전달할 수 있습니다.

# 아키텍처

![sortedSet](/assets/img/2024/241109/kafka_01.png)


# Sample Code

### 1. 서비스 등록

![sortedSet](/assets/img/2024/241109/kafka_02.png)

``` yml
version: "3.7"
services:
  postgres:
    image: debezium/postgres:13
    ports:
      - 5432:5432
    environment:
      - POSTGRES_USER=docker
      - POSTGRES_PASSWORD=docker
      - POSTGRES_DB=exampledb
    networks:
      - cdc_network

  zookeeper:
    image: confluentinc/cp-zookeeper:5.5.3
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - cdc_network

  kafka:
    image: confluentinc/cp-enterprise-kafka:5.5.3
    depends_on: [zookeeper]
    environment:
      KAFKA_ZOOKEEPER_CONNECT: "zookeeper:2181"
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_BROKER_ID: 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_JMX_PORT: 9991
    ports:
      - 9092:9092
    networks:
      - cdc_network

  debezium:
    image: debezium/connect:1.4
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: connect_configs
      OFFSET_STORAGE_TOPIC: connect_offsets
      STATUS_STORAGE_TOPIC: connect_statuses
      KEY_CONVERTER: io.confluent.connect.avro.AvroConverter
      VALUE_CONVERTER: io.confluent.connect.avro.AvroConverter
      CONNECT_KEY_CONVERTER_SCHEMA_REGISTRY_URL: http://schema-registry:8081
      CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL: http://schema-registry:8081
      CONNECT_PLUGIN_PATH: /kafka/connect
      CONNECT_SECURITY_PROTOCOL: PLAINTEXT
    depends_on: [kafka]
    ports:
      - 8083:8083
    networks:
      - cdc_network

  schema-registry:
    image: confluentinc/cp-schema-registry:5.5.3
    environment:
      - SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=zookeeper:2181
      - SCHEMA_REGISTRY_HOST_NAME=schema-registry
      - SCHEMA_REGISTRY_LISTENERS=http://schema-registry:8081
    ports:
      - 8081:8081
    depends_on: [zookeeper, kafka]
    networks:
      - cdc_network

networks:
  cdc_network:
    driver: bridge

# psql -U docker -d exampledb -w
# exampledb=# CREATE TABLE student (id integer primary key, name varchar);
# CREATE TABLE
# exampledb=# ALTER TABLE public.student REPLICA IDENTITY FULL;
# ALTER TABLE
# exampledb=# INSERT INTO student (id,name) VALUES (1, 'bob');
# INSERT 0 1
# exampledb=# select * from student
# exampledb-# ;
#  id | name
# ----+------
#   1 | bob
# (1 row)

# exampledb=# update student set name='bob2' where id =1;
# UPDATE 1
# exampledb=# select * from student;
#  id | name
# ----+------
#   1 | bob2
# (1 row)

```


### 2. Connector 등록

``` json
{
    "name": "exampledb-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "plugin.name": "pgoutput",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "docker",
      "database.password": "docker",
      "database.dbname": "exampledb",
      "database.server.name": "postgres",
      "table.include.list": "public.student"
    }
  }
```

debezium host로 curl 요청하여 구독 수행

```
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" 127.0.0.1:8083/connectors/ --data "@debezium.json"
```

### 3. Kafkacat으로 모니터링

하기 명령어로 kafkacat 모니터링

```
 docker run --tty \
 --network cdc_cdc_network \
 confluentinc/cp-kafkacat \
 kafkacat -b kafka:9092 -C \
 -s key=s -s value=avro \
 -r http://schema-registry:8081 \
 -t postgres.public.student

```

