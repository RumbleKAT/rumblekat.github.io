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

# 비교 연산자
Java와 다르게 객체를 비교할때 비교 연산자를 사용하면 자동으로 compareTo를 호출해준다.

~~~ kotlin
 val money1 = Javamoney(2_000L)
 val money2 = Javamoney(1_000L)

 if(money1 > money2){
    println("Money1이 Money2보다 금액이 큽니다.")
 }
~~~

kotlin의 등등성과 동일성
- kotlin은 동등성의 경우, == (=equals)
- kotlin은 동일성에 ===

~~~ kotlin
val money1 = JavaMoney(1_000L)
val money2 = money1
val money3 = JavaMoney(1_000L)

println(money1 === money3)
~~~

Lazy연산
- 조건문에서 앞에게 true이면 뒤에 조건은 물어보지도 않고 넘어감

in / !in
 - 컬렉션 범위가 포함
a..b
 - a부터 b까지의 범위 객체 생성

코틀린에선 연산자 오버로딩이 가능함

~~~ kotlin
val money1 = Money(1_000L)
val money2 = Money(2_000L)
println(money1 + money2) // 3_000
~~~


## 위임패턴
by의 역할은 property과 lazy 객체의 getter를 이어준다.

Lazy 객체의 getter를 어떻게 할수 있을까?
-> by 뒤에 위치한 클래스는 getValue, setValue라는 약속된 함수를 써야된다.

~~~ kotlin
class Person3{
    // name과 대응되는, 외부로 드러나지 않는 프로퍼티: Backing Property
    private val delegateProperty = LazyInitProperty{
        Thread.sleep(2_000L)
        "김수한무"
    }

    val name:String
        get() = delegateProperty.value

    //위임 패턴, Person의 getter가 호출되면 Delegate property가 대신해준다.
}

class LazyInitProperty<T>(val init: () -> T){
    private var _value: T? = null
    val value: T
        get(){
            if(_value == null)
                this._value = init()
            return _value!!
        }
    operator fun getValue(thisRef: Any, property: KProperty<*>): T{
        return value // 원래 lazy는 스레드 세이프함
    }
}
~~~

