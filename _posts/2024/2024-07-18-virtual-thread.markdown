---
layout: post
title:  "JAVA Virtual Thread"
date:   2024-07-18 21:00:00 +0900
categories: dev
---

# 들어가면서
Go에 goroutine은 경량 스레드 모델로, 기존 언어의 스레드 모델보다 더 작은 단위로 실행 단위를 나눠 컨텍스트 스위칭 비용과 Blocking 타임을 낮추는 개념입니다.
Kotlin에는 coroutine이라는 이름으로 도입이 되었고, JDK21부터 Virtual Thread가 정식적으로 릴리즈 되었습니다.

> 참고자료
https://techblog.woowahan.com/15398/
https://d2.naver.com/helloworld/1203723


# 기존 Java 스레드 모델
기존 Java의 스레드 모델은 Native Thread 모델로, Java의 유저 스레드에서 JNI를 통해 커널영역을 호출하여, 커널스레드를 생성하고 매핑하여 작업을 수행하는 형태였습니다.
즉, 유저스레드와 커널스레드 간의 관계는 1:1입니다.

여기서, Java의 스레드는 I/O, Interrupt, sleep과 같은 상황에선, block/waiting 상태가 되는데, **이때 다른 스레드가 커널 스레드를 점유하여 작업을 수행하는 것을
컨텍스트 스위칭이라고 부릅니다.**

스레드 모델은 기존 프로세스 모델을 잘게 쪼개면서, 프로세스의 공통된 부분은 공유하면서, 작은 여러 실행단위를 번갈아 수행할 수 있도록 만들었습니다.
그렇지만, Spring MVC / Tomcat을 사용하는 환경에선 요청당 1스레드를 사용하고, 요청량이 늘어날수록 컨텍스트 스위칭 비용도 기하급수적으로 늘어났습니다.

> https://github.com/openjdk/jdk21/blob/master/src/hotspot/share/runtime/javaThread.hpp#L78


# Virtual 스레드 모델

Virtual 스레드는 플랫폼 스레드와 가상 스레드로 나뉩니다. 즉, 플랫폼 스레드 위에서 여러 가상 스레드가 번갈아가면서 수행됩니다.
단, 여기서 **가상 스레드는 컨텍스트 스위칭비용이 기존의 Java 스레드 모델보다 저렴합니다.** 즉, 가상 스레드와 플랫폼 스레드의 관계는 1:N입니다.


|           | Thread  | Virtual Thread      |
|-----------|---------|---------------------|
| Stack Size| ~ 2MB   | ~10KB               |
| Creating time| ~1ms  | ~1µs               |
| Context Switching | ~100µs | ~10µs        |


Thread는 기본적으로 최대 2MB의 스택메모리 사이즈를 가지기 때문에, 컨텍스트 스위칭시, 메모리 이동량이 큽니다. 또한, 생성을 위해서 커널과 통신하여 스케줄링하기때문에 생성비용도 큽니다. 

반대로, Virtual Thread는 JVM에 의해 생성되기 때문에 시스템 콜과 같은 커널 영역의 호출은 적고, 메모리 크기가 일반 스레드의 1&에 불가합니다.

Platform Thread의 기본 스케줄러는 ForkJoinPool을 사용합니다. 이를 통해, Platform thread pool을 관리하고, Virtual Thread의 작업 분배를 수행합니다.

즉, JVM이 직접 접근하는 스레드는 플랫폼 스레드이며, 플랫폼 스레드에 마운트하여 실행하는 과정은 carrierThread에 실행대상 가상 스레드를 할당하는 방식입니다.

> https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/lang/VirtualThread.java#L131

## Virtual Thread의 동작원리

1. 실행될 Virtual Thread의 작업인 runContinuation을 carrier thread의 workQueue에서 push합니다.

2. Work queue에 있는 runContinuation들은 forkJoinPool에 의해 work stealing 방식으로 carrier thread에 의해 처리됩니다.

3. 처리된 runContinuation들은 I/O, Sleep으로 인한 interrupt나 작업 완료시, work queue에서 pop되어, park과정에 의해 다시 힙 메모리로 돌아갑니다.

JDK21부터 LockSupport에 Virtual Thread 판단 로직을 추가하여, 현재 스레드가 Virtual Thread인 경우, park/unpark 되도록하여, 기존 Thread 모델과 완벽하게 호환되는 Context Switching을 지원합니다.

## Kotlin Coroutine와 비교

Kotlin에서는 함수에 suspend로 선언하면 경량스레드와 유사하게 동작합니다.
그렇지만, Coroutine은 메서드 단위로 원하는 곳에만 경량스레드를 사용할 수 있습니다. 그리고, JDK21버전 이전에도 경량스레드를 적용할 수 있는 장점도 있습니다.

그러나, Coroutine 진입전까진 일반 Thread로 처리되고, Kotlin이 만든 suspend 확장함수를 사용해야 되기 때문에 프로덕션 코드에 변경이 필요합니다.

또한, suspend function들은 특정 지점에서 runBlocking을 사용하거나, suspend Controller를 만들어야되는데, 이는 응답을 reactive 응답으로 전환하게 됩니다.

# 주의사항
- No Pooling:
    가상 스레드는 값싼 일회용품이여서 스레드 풀을 만드는 행위는 낭비가 될수 있습니다.
- CPU Bound 작업에는 비효율:
    IO작업에는 유효하지만, CPU작업만을 수행한다면, 플랫폼 스레드만 사용하는 것보다 성능이 떨어집니다. 즉, 컨텍스트 스위칭이 자주있는 상황이 아니면 기존 스레드가 적합합니다.
- Pinned issue:
    Virtual Thread 내에서 Synchronized나, ParrallelStream 혹은 네이티브 메서드를 쓰면, park 될수 없는 상태가 되어버립니다. 이는 성능 저하를 유발할수 있습니다. 이부분은 Spring도 Synchronized 를 내부에서 사용하는 경우가 있어서 ReentrantLock으로 변환되고 있는 움직임이 있습니다.
- Thread local:
    Virtual Thread는 수시로 생성, 삭제되기에, 항상 작게 유지하는 것이 중요합니다.








