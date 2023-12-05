---
layout: post
title:  "Kotlin Type"
date:   2023-12-05 21:35:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

코틀린은 선언된 기본값을 보고 타입을 추론한다.
코틀린에서 타입 변환은 **명시적**으로 되어야된다.

> java
~~~ java
int number1 = 4;
long number2 = number1;

System.out.println(number1 + number2); //long 타입으로 자바에서 자동 변환해준다.
~~~

> kotlin
~~~ kotlin
val number1: Int = 4
val number2: Long = number1.toLong() // 명시적으로 타입을 명시
~~~

변수가 nullable이라면 적절한 처리가 필요

~~~ kotlin
val number1: Int? = 3
val number2: Long = number1?.toLong() ?: 0L
~~~

일반 타입의 경우, is는 Java의 InstanceOf와 같음
~~~ kotlin
fun printAgeIfPerson(obj: Any){
    if(obj is Person){
        // val person = obj as Person
        // println(person.age)
        println(obj.age) // smart casting
    }
}
~~~

만약에 obj가 null이면 null이 나옴, Person이라면 Person으로 나온다.

~~~ kotlin
val person = obj as? Person
~~~

value as Type => Type으로 타입 캐스팅한다.

# Kotlin의 특이타입
- Any = Java의 Any / Any의 hashCode, toString, equals가 있음
- Unit = Java의 void와 동일한 역할. void와 다르게 Unit은 그 자체로 타입 인자로 사용 가능
- Noting = 함수가 정상적으로 끝나지 않았다는 사실을 표현하는 역할 (무조건 예외를 만드는 것)

# String Interpolation / String Indexing
- ${변수}를 사용함
- """ 을쓰면 여러 줄의 문자열을 작성할 수 있음

~~~ kotlin
val str = "abc"
str[0] = "a"
~~~
