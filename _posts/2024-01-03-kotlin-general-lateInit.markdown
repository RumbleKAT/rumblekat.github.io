---
layout: post
title:  "Kotlin Type"
date:   2024-01-03 23:35:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## not Null
lateinit과 비슷한 역할 primitive에서 사용

~~~ kotlin
class Person{
    var age: Int by notNull()
}
~~~

## obervable
onChange 새로 프로퍼티가 생성될때 쓴다.

~~~ kotlin
class Person4{
    var age: Int by Delegates.observable(20) {
        _, oldValue, newValue -> 
        println("이전 값: ${oldValue}, 현재 값: ${newValue}")
    }
}

~~~

## vetoable
observable과 매우 유사하다. onChange 함수가 Boolean을 반환한다. true를 반환하면 값이 변경, false는 값이 변경되지 않는다.

## 또 다른 프로퍼티로 위힘하기
프로퍼티 앞에 ::을 붙이면, 위임 객체로 사용할 수 있다.

~~~ kotlin
class Person{
    @Deprecated("age를 사용하세요!", ReplaceWith("age"))
    var num: Int = 0

    var age: Int by this::num
}
~~~

## map
getter가 호출, 결과가 있으면 결과를 리턴함

~~~ kotlin
val person = Person(mapOf("name" to "ABC"))
println(person.age) // 예외 발생
~~~