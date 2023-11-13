---
layout: post
title:  "Kotlin coroutine #4"
date:   2023-11-13 21:21:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## Structured Concurrency

수많은 코루틴이 유실되지 않도록 보장하는 기술, 
단 **CancellationExceptio**n은 정상적인 취소로 간주하기 때문에, 부모 코루틴에게 전파되지 않음

> 즉, 부모 코루틴의 다른 자식 코루틴을 취소 시키지 않는다.

~~~ kotlin
fun main(): Unit = runBlocking {
    launch {
        delay(500L)
        printWithThread("A")
    }

    launch{
        delay(600L)
        throw IllegalArgumentException("코루틴 실패!")
    }
}
~~~

## CoroutineScope

launch와 async는 CoroutineScope의 확장함수이다. 
지금까지는 runBlocking이 코루틴과 루틴의 세계를 이어주며, CoroutineScope를 제공함.

CoroutineContext를 가진다. 코루틴과 관련된 여러가지 데이터를 가지고 있음. 코루틴이 어떤 스레드에 배정될지 정함.

- CoroutineScope : 코루틴이 탄생할수 있는 영역
- CoroutineContext : 코루틴과 관련된 데이터를 보관
    - Map + Set을 합쳐놓은 상태, Key - Value로 데이터 저장(즉, 같은 데이터는 유일함)
- CoroutineDispatcher: 코루틴을 스레드에 배정하는 역할
    - Dispatchers.Default : 기본
    - Dispathcers.io: 입출력 전문
    - Dispathcers.main: 보통 UI 컴포넌트를 조작하기 위한 디스페처, 의존성 필요
    - asCoroutineDispatcher: Executors

자식 코루틴은 부모 코루틴의 CoroutineScope를 기반으로 자신의 내용을 만듬

~~~ kotlin
suspend fun main(){
    val job = CoroutineScope(Dispatchers.Default).launch {
        delay(1_000L)
        printWithThread("Job 1")
    }

    job.join()
}
~~~

독립된 스코프를 만들어서 여러 코루틴을 한번에 종료시킬수있다.

~~~ kotlin
class AsyncLogic {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun doSomething(){
        scope.launch {
            // do somting
        }
    }
    
    fun destroy(){
        scope.cancel()
    }
}
~~~


## asCoroutineDispatcher 사용 예시

원하는 ThreadPool을 생성하여, 코루틴을 원하는 ThreadPool에서 동작하게 지정할 수 있다.

~~~ kotlin
fun main(){
    CoroutineName("나만의 코루틴") + Dispatchers.Default
    val threadPool = Executors.newSingleThreadExecutor()
    CoroutineScope(threadPool.asCoroutineDispatcher()).launch {
        printWithThread("새로운 코루틴")
    }
}
~~~

결과 
~~~
[pool-1-thread-1] 새로운 코루틴
~~~
