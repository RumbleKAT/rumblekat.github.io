---
layout: post
title:  "Kotlin coroutine #3"
date:   2023-11-08 22:01:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

Coroutine을 쓰면서 중첩된 코루틴 선언은 부모, 자식 관계가 생성된다.
이를 제외 하려면 CoroutineScope을 넣어야 한다.

~~~ kotlin
fun main(): Unit = runBlocking {// 부모
    val job1 = CoroutineScope(Dispatchers.Default).launch { // 메인스레드가 아닌 다른 스레드에서 처리
        delay(1_000L)
        printWithThread("Job 1")
    }

    val job2 = launch {//자식
        delay(1_000L)
        printWithThread("Job 2")
    }
}

~~~

## launch와 async의 예외 발생 차이

- launch: 예외가 발생하면, 예외를 출력하고 코루틴이 종료
- async: 예외가 발생해도, 예외를 출력하지않고, await()을 사용하면 예외가 예외선언한 스레드에서 출력

~~~ kotlin

fun main(): Unit = runBlocking {
//    val job = CoroutineScope(Dispatchers.Default).launch {
//        throw IllegalArgumentException()
//    }
    // launch는 종료된다.

    val job1 = CoroutineScope(Dispatchers.Default).async {
        throw IllegalArgumentException()
    }
    // async는 종료되지 않는다.

    delay(1_000L)
    job1.await() // main 스레드에서 예외를 발생한다.
}

~~~

## 자식 코루틴의 예외는 부모에게 전달된다.

~~~ kotlin

fun main(): Unit = runBlocking {
    val job = async{
        throw IllegalArgumentException()
    } // 자식 코루틴의 예외는 부모에게 전달된다.  
}

~~~

## SupervisorJob() => 예외를 막는 방법

~~~ kotlin
fun main(): Unit = runBlocking {
    val job = async(SupervisorJob()){
        throw IllegalArgumentException()
    } // 자식 코루틴의 예외는 부모에게 전달된다.
    // job.await()
}
~~~

## 코루틴에서 예외를 잡는 방법

- try, catch, finally
- CoroutineExceptionHandler => launch에만 사용가능하고, 부모 코루틴이 있으면 동작하지 않는다.

~~~ kotlin
    fun main(): Unit = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler{ _, throwable ->
        printWithThread("예외")
        throw throwable
    }

    val job = CoroutineScope(Dispatchers.Default).launch(exceptionHandler){
        throw IllegalArgumentException()
    }

    delay(1_000L)
}

~~~

## 코루틴 취소 예외 정리

- 발생한 예외가 CancellationException인 경우, **취소**, 부모 코루틴에게 전파하지 않는다.
- 그외의 예외는 부모 코루틴에게 전파한다.

## Coroutine의 Life Cycle

1. NEW
2. ACTIVE 
2-1. CANCELLING **예외 발생**
2-2. CANCELLED
3. COMPLETING
4. COMPLETED
