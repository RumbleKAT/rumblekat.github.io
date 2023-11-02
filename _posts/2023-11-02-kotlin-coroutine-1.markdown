---
layout: post
title:  "Kotlin coroutine #1"
date:   2023-11-02 21:07:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## 루틴과 코루틴의 차이
- 루틴은 시작되면, 끝날때까지 멈추지 않는다.
- 루틴은 한번 끝나면 루틴 내의 정보가 사라진다.

- 코루틴은 중단되었다가 재개될 수 있다.
- 중단되었더라도, 루틴 내의 정보가 사라지지 않는다.

~~~ kotlin

package coroutine

import kotlinx.coroutines.*

suspend fun newRoutine(){ // 다른 suspend fun을 호출할수있다.
    val num1 = 1
    val num2 = 2
    yield() // 스레드를 양보한다. 중단과 재개
    printWithThread("${num1 + num2}") // 루틴이 중단되었다가 메모리 상에서 불러와서 다시 재개한다.
}

fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    printWithThread("START")
    launch { // 반환값이 없는 코루틴을 만듬
        newRoutine() // 바로 수행하지 않음
    }
    yield() // new 루틴에서 양보해서 end를 호출
    printWithThread("END")
}

fun printWithThread(str: Any){
    println("[${Thread.currentThread().name}] $str")
}

~~~

## 결과값

runBlocking 하면서 1번 코루틴을 수행하면서
launch로 2번 코루틴을 수행한다. 그렇지만, yield 선언으로 중간에 메모리를 저장한채, 다시 수행한다.

~~~
[main @coroutine#1] START
[main @coroutine#1] END
[main @coroutine#2] 3
~~~

## 프로세스와 스레드 그리고 코루틴

스레드는 프로세스의 종속. 스레드가 프로세스를 바꿀수 없다.
코루틴의 코드가 실행되려면, 스레드가 있어야만한다.
중단되었다가 재개될때 다른 스레드에 배정될 수 있다.

코루틴의 코드를 어떤 스레드건 가져가면됨.

프로세스는 독립된 메모리를 가지고있음 
context 스위칭시 모두 바껴야됨

스레드는 heap area를 공유, 스택 area만 교체

코루틴은 동일스레드에서 수행되면, context switching이 다름
메모리 교체가 따로 없다. ->  **동시성**

코루틴은 스스로 자리를 양보할 수 있다. **비선점형**

## runBlocking

새로운 코루틴을 만들고, 루틴과 코루틴을 연결 
스레드를 블록킹시킴

~~~ kotlin

fun main() { // 코루틴세계로 연결한다.
    runBlocking {
        printWithThread("START")
        launch {
            delay(2_000L) // yield
            printWithThread("LAUNCH END")
        }
    }
    printWithThread("END")
}

~~~

## 결과값

~~~
[main @coroutine#1] START
[main @coroutine#2] LAUNCH END
[main] END
~~~

## launch

반환값이 없는 코드를 실행
객체 job을 반환 할수 있음

### start

~~~ kotlin
fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val job = launch(start = CoroutineStart.LAZY) {
        printWithThread("Hello launch")
    }

    delay(1000)
    job.start()
}
~~~

### cancel

~~~ kotlin
fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val job = launch {
        (1..5).forEach{
            printWithThread(it)
            delay(300)
        }
    }

    delay(1000)
    job.cancel()
}
~~~

~~~
[main @coroutine#2] 1
[main @coroutine#2] 2
[main @coroutine#2] 3
[main @coroutine#2] 4
~~~

### join

await와 비슷함

~~~ kotlin
fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val job1 = launch {
        delay(1000)
        printWithThread("job 1")
    }
    job1.join()
    val job2 = launch {
        delay(1000)
        printWithThread("job 2")
    }
    job2.start()
}
~~~

### async

결과를 반환 할수 있음 like Promise
여러 API를 한번에 호출할 수 있음


~~~ kotlin

fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val job1 = async {
        3 + 5
    }
    val eight = job1.await()
    print(eight)
}

~~~

~~~ kotlin

fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val time = measureTimeMillis {
        val job2 = async { apiCall2() }
        val job1 = async { apiCall1() }
        printWithThread(job1.await() + job2.await())
    }
    printWithThread("소요 시간: $time ms")
}

/*
fun main(): Unit = runBlocking { // 코루틴세계로 연결한다.
    val job1 = async { apiCall1() }
    val job2 = async { apiCall2(job1.await()) }
    
}

suspend fun apiCall1(): Int{
    delay(1000L)
    return 1
}

suspend fun apiCall2(num : Int): Int{
    delay(1000L)
    return 2 + num
}
*/

suspend fun apiCall1(): Int{
    delay(1000L)
    return 1
}

suspend fun apiCall2(): Int{
    delay(1000L)
    return 2
}

/*
[main @coroutine#1] 3
[main @coroutine#1] 소요 시간: 1065 ms
*/
~~~

## Coroutine.LAZY

Coroutine.LAZY는 실제 코루틴의 실행이 완료될때까지 기다리므로 시간이 더 걸림

~~~ kotlin
    val job2 = async(start= CoroutineStart.LAZY) { apiCall2() }
~~~


