---
layout: post
title:  "Kotlin Operator Overloading"
date:   2024-02-04 19:35:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## 연산자 오버로딩의 특징
- operator가 들어있다.
- 함수의 이름과 함수의 파라미터가 정해져있다.
- unaryMinus는 다른 타입도 허용되나, dec, inc는 같은 타입을 전달

~~~ kotlin
data class Point(
    val x: Int,
    val y: Int,
){
    fun zeroPointSymmetry():Point = Point(-x,-y)
    operator fun unaryMinus(): Point {
        return Point(-x,-y)
    }
    operator fun inc(): Point{
        return Point(x+1, y+1)
    }
}

fun main() {
    var point = Point(20,-10)
//    println(Point(20,-10).zeroPointSymmetry())
    println(-point)
    println(++point)
}
~~~

~~~ kotlin
data class Days(val day: Long)

operator fun LocalDate.plus(days: Days): LocalDate {
    return this.plusDays(days.day)
}

val Int.d: Days
    get() = Days(this.toLong())

fun main(){
    LocalDate.of(2023, 1,1).plusDays(3) // 2023-01-04
    LocalDate.of(2023, 1,1)+ Days(3)
    LocalDate.of(2023, 1,1)+ 3.d
}
~~~

복합 대입 연산자는 조금 복잡하다.
- 복합 대입연산자 오버로딩이 있으면 그함수를 적용하고, 없으면 var 변수라면 산술연산자로 업데이트를하고, val변수라면 에러 발생한다.

~~~ kotlin
fun main(){
    var list = listOf("A","B","C")
    list += "D" // 복사를 하고 새로 추가
}
~~~

함수호출도 하나의 연산자다.

> **연산자 오버로딩**은 의미에 맞게 사용하는 것이 중요하다.

