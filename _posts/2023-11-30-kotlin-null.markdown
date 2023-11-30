---
layout: post
title:  "Kotlin Null"
date:   2023-11-30 20:33:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

Koltin에서의 null 체크
kotlin에서는 null이 가능한 타입을 완전히 다르게 취급한다.

자바 형식으로만 작성했을 때

~~~ kotlin
fun startsWithA1(str: String?): Boolean{
  if(str == null){
    throw IllegalArgumentException("null이 들어왔습니다.")
  }
  return str.startsWith("A")
}

fun startsWithA2(str: String?): Boolean?{
  if(str == null) return null
  return str.startsWith("A")
}

fun startsWithA3(str: String?):Boolean{
  if(str == null) return false
  return str.startsWith("A")
}
~~~

## Safe call

null의 length는 null이다.
safe call은 null이면 전체가 null이기 때문에

~~~ kotlin
val str: String? = "ABC"
str.length // 불가능
str?.length // 가능
~~~

## Elvis 연산자

앞의 연산 결과가 null이면 뒤의 값을 사용

~~~ kotlin
  val str: String? = null
  println(str?.length ?: 0) // 0
~~~

코틀린 형식으로 작성했을 때

~~~ kotlin
fun startsWithA1(str: String?): Boolean{
    // null이 아니면 safe call이 수행된다.
    return str?.startsWith("A") ?: throw IllegalArgumentException("null이 들어왔습니다.")
}

fun startsWithA2(str: String?): Boolean?{
  return str?.startsWith("A")
}

fun startsWithA3(str: String?):Boolean{
    return str?.startsWith("A") ?: false
}
~~~

early return에서 사용할수 있음

## !!

nullable type이지만, 아무리 생각해도 null이 될수 없는 경우.
혹시 null이 들어오면, NPE가 생김
~~~ kotlin
fun startsWith(str: String?): Boolean{
    return str!!.startsWith("A")
}
~~~

## 플랫폼 타입
자바의 annotation 정보를 코틀린이 이해한다.

코틀린이 null 관련 정보를 알수 없는 경우, Runtime 에러가 날수 있다.
**wrapping해서 단일 지점으로 관리**하는 것을 추천