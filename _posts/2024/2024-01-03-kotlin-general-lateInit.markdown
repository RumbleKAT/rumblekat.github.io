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

# 위임 객체
코틀린은 기본적으로 위임객체용 인터페이스를 제공한다.
잠깐 사용할 위임객체도 만들수있음, 아래처럼 익명 클래스에도 사용가능

~~~ kotlin
val status: String by object : ReadOnlyProperty<Person3, String>{
    private var isGreen: Boolean = false
    override fun getValue(thisRef: Person3, property: KProperty<*>): String{
        return if(isGreen){
            isGreen = true
            "Happy"
        } else {
            isGreen = false
            "Sad"
        }
    }
} 

~~~

위임 프로퍼티와 위임 객체를연결할때 로직을 넣으면,
프로퍼티 이름을 직접 넣어줘야 하니, 번거롭고 추가적인 작업이 필요한 문제점이 있다.

## provideDelegate 함수
DelegateProvider에 내용을 추가하여 제어 가능
> 비슷하게 propertyDelegateProvider로 됨

~~~ kotlin
class Person5{
    val name by DelegateProvider("song")
    val country by DelegateProvider("korea")
}

class DelegateProvider(
    private val initValue: String
){
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>): DelegateProperty{
        if(property.name != "name"){
            throw IllegalArgumentException("only name field have to")
        }
        return DelegateProperty(initValue)
    }
}
~~~

위임 클래스는 위임 프로퍼티와 원리가 다르다.

~~~ kotlin
interface Fruit{
    val name: String
    val color:String
    fun bite()
}

open class Apple: Fruit{
    override val name: String
        get() = "사과"
    override val color: String
        get() = "빨간색"
    override fun bite(){
        print("사과 톡톡~")
    }
}

class GreenApple2: Apple{
    override fun bite(){
        print("초록사과"~)
    }
}
// 합성을 이용, 클래스 위임을 사용할 수있다.
class GreenApple3: Apple(
    private val apple:Apple
): Fruit by apple{
    override val color: String
        get() = "초록색"
}


~~~

# Iterable
각 단계마다 중간 collection이 임시로 생성된다.
대용량 데이터를 다루려면 sequence를 적용한다.

1. sequence는 각 단계가 모두 수행이 안될수있다.
2. 한 원소에 대해서 모든 연산을 수행하고, 다음 원소로 넘어간다. (세로로 연산)
3. 최종 연산이 나오기 전까지 계산 자체를 미리하지 않는다.

> 이를 **지연** 연산이라고 한다.

~~~ kotlin
data class MyFruit(
    val name: String,
    val price: Long,
)

fun main() {
    val fruits = listOf(
        MyFruit("사과", 1000L),
        MyFruit("바나나", 3000L),
    )

    val avg = fruits.asSequence()
        .filter{ it.name == "사과" }
        .map{ it.price }
        .take(10_000)
        .average()

    println(avg)
}

~~~

데이터가 많지않으면, Iterable이 유리, 많으면 Sequence
