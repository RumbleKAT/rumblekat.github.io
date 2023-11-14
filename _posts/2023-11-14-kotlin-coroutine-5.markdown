---
layout: post
title:  "Kotlin coroutine #5"
date:   2023-11-14 23:16:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## suspending function

suspend가 붙은 함수, 다른 suspend를 붙은 함수를 호출할수있다. like **async**
launch의 마지막 block 파라미터가 suspend 함수

suspend function 함수는 코루틴이 중지 되었다가 **재개** 될 수 있는 지점

여러 비동기 라이브러리를 사용할수 있도록 쓴다. 동기식처럼 순서를 보장

~~~ kotlin
fun main(): Unit = runBlocking {
    val result1 = async {
        call1()
    }

    val result2 = async {
        call2(result1.await())
    }

    printWithThread(result2.await())
}

fun call1(): Int{
    Thread.sleep(1_000L)
    return 100
}

fun call2(num: Int): Int{
    Thread.sleep(1_000L)
    return num * 2
}
~~~

위의 코드는 아래와 같이 CompletableFuture를 설정해서 쓸수 도 있음

~~~ kotlin
fun main(): Unit = runBlocking {
    val result1 = call1()
    val result2 = call2(result1)

    printWithThread(result2)
}

suspend fun call1(): Int{
    return CoroutineScope(Dispatchers.Default).async {
        Thread.sleep(1_000L)
        100
    }.await()
}

suspend fun call2(num: Int): Int{
    return CompletableFuture.supplyAsync {
        Thread.sleep(1_000L)
        num * 2
    }.await()
}

~~~

- CoroutineScope: 추가적인 코루틴을 만들어주고, 주어진 함수 블록이 바로 수행. 만들어진 코루틴이 완료시, 다음코드로 넘어간다.

~~~ kotlin

fun main(): Unit = runBlocking {
    printWithThread("START")
    printWithThread(calculateResult()) // 동기처럼 수행된다.
    printWithThread("END")
}

suspend fun calculateResult(): Int = coroutineScope {
    val num1 = async {
        delay(1_000L)
        10
    }

    val num2 = async {
        delay(1_000L)
        120
    }

    num1.await() + num2.await()
}
~~~

~~~ 
[main] START
[main] 130
[main] END
~~~

- WithContext : CoroutineScope랑 비슷함

~~~ kotlin
suspend fun calculateResult():Int = withContext(Dispatchers.Default)
~~~

- WithTimeout, WithTimeoutOrNull : 주어진 시간내에 처리 여부에 따라 값이 다름

~~~ kotlin
fun main(): Unit = runBlocking {
    val result: Int?   = withTimeoutOrNull(1_000L){
        delay(1_500L)
        10 + 20
    }
    if (result != null) {
        printWithThread(result)
    }
}
~~~


## Continuation

Continuation을 callback으로 활용
CPS = Continuaction Passing Style

내부적으론 CoroutineContext와 resumeWith가 있음

~~~ kotlin
package coroutine

import kotlinx.coroutines.*

interface Continuation{
    suspend fun resumeWith(data: Any?)
}

class UserService{
    private val userProfileRepository = UserProfileRepository()
    private val userImageRepository = UserImageRepository()

    private abstract class FindUserContinuation(): Continuation{
        var label = 0
        var profile: Profile? = null
        var image: Image? = null
    }

    suspend fun findUser(userId: Long, cont: Continuation?): UserDto {
        val sm = cont as? FindUserContinuation ?: object : FindUserContinuation() {
            override suspend fun resumeWith(data: Any?) {
                when(label){
                    0 -> {
                        profile = data as Profile
                        label = 1
                    }
                    1 -> {
                        image = data as Image
                        label = 2
                    }
                }
                findUser(userId, this)
            }
        }

        when(sm.label){
            0 -> {
                println("프로필을 가져오겠습니다.")
                val profile = userProfileRepository.findProfile(userId, sm)
            }
            1 -> {
                println("이미지를 가져오겠습니다.")
                val image = userImageRepository.findImage(sm.profile!!, sm)
            }
        }
        return UserDto(sm.profile!!, sm.image!!)
    }

}

class UserImageRepository {
    suspend fun findImage(profile: Profile, cont: Continuation) {
        delay(1_000L)
        cont.resumeWith(Image())
    }
}

class Profile
class Image
class UserDto(
    profile: Profile, image:Image
)
class UserProfileRepository {
    suspend fun findProfile(userId: Long, cont: Continuation) {
        delay(1_000L)
        cont.resumeWith(Profile())
    }
}

suspend fun main() {
    val service = UserService()
    printWithThread(service.findUser(1L, null))
}


fun printWithThread(str: Any){
    println("[${Thread.currentThread().name}] $str")
}
~~~