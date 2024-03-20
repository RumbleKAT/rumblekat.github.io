---
layout: post
title:  "Kotlin coroutine #2"
date:   2023-11-07 23:01:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

Coroutine을 쓰면서 취소는 자원활용에 연관된다.
Suspend 관련 함수를 사용하여 취소를 적용할수 있음

~~~ kotlin
fun main(): Unit = runBlocking {
    val job1 = launch {
        delay(1_000L)
        printWithThread("Job 1")
    }
    val job2 = launch {
        delay(1_000L)
        printWithThread("Job 2")
    }
    delay(100)
    job1.cancel()
}

fun printWithThread(str: Any){
    println("[${Thread.currentThread().name}] $str")
}
~~~

## 수행결과
~~~
[main] Job 2

~~~

~~~ kotlin

fun main(): Unit = runBlocking {
   val job1 = launch {
       delay(10) // 0.01 초만에 수행된다.
       printWithThread("Job 1")
   }
    delay(100) // 0.1초를 기다리는 동안
    job1.cancel()
}

fun printWithThread(str: Any){
    println("[${Thread.currentThread().name}] $str")
}

~~~

**단** 취소에 협력적이지 않으면 계속 수행될수있다.
**즉 suspend 함수가 없으면 계속 수행된다.**

## CancellationException

코루틴 스스로 본인의 상태를 확인해, 취소 요청을 받았으면, CancellationException을 던진다. 

- isActive: 현재 코루틴이 활성화 되어 있는지, 취소 신호를 받았는지 
- Dispatchers.Default를 쓰는 이유는 throw CancellationException할때 main 스레드에서 처리하면 안되서 별도의 스레드를 띄워서 처리한다. (main 스레드는 이를 처리 못함)

~~~ kotlin

fun main(): Unit = runBlocking {
  val job = launch(Dispatchers.Default) { // main 스레드와 다름
      var i = 1
      var nextPrintTime = System.currentTimeMillis()
      while(i<= 5){
          if(nextPrintTime <= System.currentTimeMillis()){
              printWithThread("${i++}출력")
              nextPrintTime += 1_000L
          }
          
          if(isActive){
              // 현재 취소 요청을 받았는지, 활성화 상태인지 알수있음
             throw CancellationException() //명백히 취소시킴
          }
      }
  }
    delay(100L)
    job.cancel()
}

~~~

## try catch로 잡으면 취소 안됨..

~~~ kotlin
fun main(): Unit = runBlocking {
    val job = launch {
        try{
            delay(1_000L)
        }catch (e: CancellationException){
            // do nothing..
        }
        printWithThread("delay에 의해 취소되지 않았다.")
    }
    delay(100L)
    printWithThread("취소 시작")
    job.cancel()
}
~~~