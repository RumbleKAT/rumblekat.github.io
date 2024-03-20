---
layout: post
title:  "Kotlin Generic #2"
date:   2023-11-26 15:20:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

Kotlin의 In, out 구문은 Java에 있는 와일드 카드 타입과 대응된다.

~~~ java
List<Integer> ints = List.of(1,2,3)
List<? extend Number> nums = ints
~~~
특정 지점에 변성을 주는 방식은 유용하다.

~~~ kotlin 
<out T> = <? extends T>
<in T> = <? super T>
~~~

## 제네릭 클래스 자체를 공변하게 만드는 방법
변성을 주는 위치 2가지

- declaration site variance
~~~ kotlin
class Cage3<out T>{
    ...
}
~~~

- use site variance
~~~ kotlin
fun moveFrom(cage: Cage2<out T>){
    this.animals.addAll(cage.animals);
}
~~~

## in 선언지점 변성 활용 예시
- Comparable

## out 선언지점 변성 활용 예시
- List 
@UnsafeVarianace -> 타입이 안전하다고 생각하는 경우에 넣음
개발자가 안전함을 보장할 수 있어야됨

# 제네릭 제약과 제네릭 함수
타입 파라미터에 제한 조건을 주는 법 = 제네릭 제약

~~~ kotlin 
class Cage5<T: Animal>{

}
~~~

제한 조건을 여러개 두고 싶다면? 
~~~ kotlin 
class Cage5<T>(
    private val animals: MutableList<T> = mutableListOf()
) where T: Animal, T: Comparable<T>{

    fun printAfterSorting(){
        this.animals.sorted()
        .map { it.name }
        .let { println(it) } 
    }
}
~~~
이런 경우 동물들을 정렬하는 함수를 만들수 있음


