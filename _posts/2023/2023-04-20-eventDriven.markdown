---
layout: post
title:  "EventDriven - Spring Boot"
date:   2023-04-20 21:58:00 +0900
categories: dev
---

# 멱등성
- 동일한 요청을 한번 보내는 것과, 여러번 연속으로 보내는 것이 같은 효과가 지닌것.
Form의 Submit의 경우, 여러번 요청이 가는 것을 방지하기 위해 사용

POST는 근본적으로 멱등성, 안정성 보장 안함
GET은 멱등성, 안정성 보장함
PUT,DELETE은 멱등성은 보장, 안정성은 보장하지않아서, 로직을 안전하게 구현해야됨

# Event Driven Architecture
시스템의 복잡성을 줄이고, 유연성과 확장성 향상 가능
비동기로 동작하여 응답 대기로 인한 시스템 부하가 적음

## 이벤트란?
- 발생한 사건을 표현하는 메시지 방식
- 변경 불가능함
- 이벤트를 생성한 서비스는 처리에 관해서 관여하지 않는다.

## 왜 Event Driven인가?
기존 MSA의 경우, 서비스가 고도화됨에 따라 상호간의 연결이 복잡해짐
서비스들의 실행이 직렬이고 동기식으로 실행되었다.

반면, EDA의 경우, 프로세스가 종속되어 있지않고, 단지 메시지 브로커에 연결해 놓으면 됨 
동기식 프로세스가 일부 필요한 경우도 설계 가능하다.

# Message Broker
표준 메시지 브로커
- RabbitMQ

로그베이스 메시지 브로커
- kafka (이벤트를 서비스에 전달한 뒤에도 계속 보관하여, 나중에 다시 볼 수 있다.)

## Pub Sub 구조
- 메시지 브로커를 통해 통신하는 대표적인 비동기 메시지 패턴

    - Topic: 단체 채팅방
    - Publish: 메시지를 발행
    - Subscribe: 수신하고자 하는 Topic에 미리 가입하여, 해당 Topic으로 publish되는 메시지를 수신한다.

# CloudStream
메시징 시스템과 연결된 확장성이 뛰어난 이벤트 기반 마이크로서비스를 구축하기 위한 프레임워크
- 어떤 메시지 브로커를 사용하더라도, 코드는 동일하다.

    - Input: 메시지 수진
    - Output: 메시지 송신
    - Binder: 특정 Middleware(Message Broker)로 메시지를 전송한다.

## 로직 순서
비즈니스 로직 실행 -> 클라우드 스트림(source, 메시지를 보내는 쪽) -> Message Broker -> 클라우드 스트림(sink) -> 비즈니스 로직 
메시지를 수신할땐, CloudStream에선 반대로 blinder->channel->sink순으로 간다.

![cloudstream](https://images.velog.io/images/wodyd202/post/a476c7a5-40d4-4ba2-bbe0-67935985d48e/0_nHsMfIYBldGLQVO7.png)

![source-sink](https://qiita-image-store.s3.amazonaws.com/0/1852/015157ad-dc48-5d53-a869-6174f077b32b.png)

