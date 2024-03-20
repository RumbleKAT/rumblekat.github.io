---
layout: post
title:  "Kotlin Annotation"
date:   2024-03-20 23:35:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## Retention
어노테이션이 저장되고 유지되는 방식을 제어한다.

- SOURCE: 어노테이션이 컴파일 때만 존재
- BINARY: 어노테이션이 런타임 때도 있지만, 리플렉션을 쓸 수 없다.
- RUNTIME: 어노테이션이 런타임 때 존재하고, 리플렉션을 쓸 수 있다.
**기본값**

## Target
어노테이션을 적용할 대상
- @Target을 명시하지 않으면, 거의 대부분 붙일 수 있다.

~~~ kotlin
@Target(AnnotationTarget.CLASS)
annotation class Shape(
    val texts: Array<String>
)
~~~


## KClass 
코드로 작성한 클래스의 정보를 가지고 있는 클래스

~~~ kotlin
val clazz::KClass<Annotation> = Annotation::class
~~~

## 어노테이션을 사용하는 방법
위치가 애매한 경우. use-site target(field, get, set....)을 사용한다.
 param > property > field 순서대로 붙는다.
~~~ kotlin
class Hello(@get:Shape val name: String)
~~~

## Repeatable
Repaetable을 붙이면 반복적으로 사용가능

~~~ kotlin
annotation class Executable

@Executable
class Reflection{
    fun a(){

    }
    fun b(n:Int){

    }
}

fun executeAll(obj:Any){

}

fun main(){
    // KClass<T> Class<T>
    val kClass: KClass<Reflection> = Reflection::class
    val ref = Reflection()
    val kClass2: KClass<out Reflection> = ref::class

    // Kotlin과 Java Reflection 기능을 둘다 사용 가능
    // inner class나 inline function 같은 기능은 kotlin

    val kType: KType = GlodFish::class.createType()
    val goldFish = GlodFish("금붕어")
    goldFish::class.members.first { it.name == "print" }.call(goldFish) // 클래스 객체를 넣어줘야됨
}

class GlodFish(val name: String){
    fun print(){
        println("금붕어 이름은 ${name}입니다.")
    }
}

fun castToGoldFish(obj: Any):GlodFish{
    return GlodFish::class.cast(obj)
}
~~~

## Kotlin의 주요 리플렉션 객체 구조

1. KCallable: 호출될 수 있는 언어요소를 표현
2. KFunction: 코틀린에 있는 함수를 표현
3. KProperty: 코틀린에 있는 프로퍼티를 표현

~~~ kotlin
@Target(AnnotationTarget.CLASS)
annotation class Executable

@Executable()
class Reflection{
    fun a(){
        print("A입니다.")
    }
    fun b(n:Int){
        print("B입니다.")
    }
}

fun executeAll(obj:Any){
    val kClass = obj::class
    if(!kClass.hasAnnotation<Executable>()){
        return
    }
    val callableFunctions = kClass.members.filterIsInstance<KFunction<*>>()
        .filter { it.returnType == Unit::class.createType() }
        .filter { it.parameters.size == 1 && it.parameters[0].type == kClass.createType()}

    callableFunctions.forEach { function ->
        function.call(kClass.createInstance())
    }
}

fun main(){
    executeAll(Reflection())
}
~~~