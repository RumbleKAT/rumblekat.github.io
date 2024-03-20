---
layout: post
title:  "Kotlin currying"
date:   2024-01-28 15:35:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

## Kotlin 람다식 예시

람다식으로 표현하는 방식과 익명함수로 표현하는 방식 2가지가 있음

~~~ kotlin
fun compute(num1: Int, num2: Int, op: (Int,Int) -> Int): Int{
    return op(num1, num2)
}

fun main() {
    // 람다식
    println(compute(3,5) {a,b -> a+b})

    // 익명함수
    compute(5,3, fun(a:Int,b:Int) = a + b)
}
~~~

위의 방식을 **함숫값**, **함수 리터럴**이라고 한다.

람다식은 반환 타입을 적을수 없고, 익명함수는 반환타입을 적을수 있다.

~~~ kotlin

fun main() {
    iterate(listOf(1,2,3,4,5), fun(num){
        if(num == 3){
            return
        }
        println(num)
    })
}

fun iterate(numbers: List<Int>, exec:(Int) -> Unit){
    for(number in numbers){
        exec(number)
    }
}

~~~

**람다식** 에선 하위 코드가 작성이 안됨 -> return을 쓸수 없다.

~~~ kotlin
fun main() {
    iterate(listOf(1,2,3,4,5)){
        num -> if(num == 3) return //return은 가장 가까운 fun을 종료하는 명령어
        println(num)
    }
}

~~~

## 함수 파라미터의 기본값 응용
좀더 객체 지향적으로 작성하면 아래처럼 응용할 수 있다.
Java에서는 비슷한 구조를 위해 BiFunction 인터페이스를 사용하지만,
Kotlin에선 함수가 1급 시민이니, 함수를 바로 사용할 수 있다.

~~~ kotlin
fun main() {
    println(calculate(1,5, Operator.PLUS))
}

fun calculate(num1:Int, num2: Int, oper:Operator) = oper.calcFun(num1,num2)

enum class Operator(
    private val oper: Char,
    val calcFun: (Int, Int) -> Int,
){
    PLUS('+', {a,b->a+b}),
    MINUS('-', {a,b->a-b}),
    MULTIPLY('*', {a,b->a*b}),
    DIVIDE('/', {a,b ->
            if(b == 0) throw IllegalArgumentException()
            else a/b
    })
}
~~~

## 확장함수의 타입 
확장 함수를 사용하는 케이스는 아래와 같이 표현 가능
함수를 변수처럼 사용할때마다 FunctionN이 늘어남

~~~ kotlin
fun main() {
    val add = fun Int.(other: Long): Int = this + other.toInt()
    println(add.invoke(1,10)) // 11
    println(add(1,10)) // 11
    1.add(10L) // 11
}
~~~

## 고차함수의 단점
오버헤드가 존재함. -> **inline 함수**로 극복 가능

~~~ kotlin
var num = 5
    num += 1
    val plusOne:() -> Unit = { num += 1 } // closure를 쓰는 경우엔 컴파일이 다름
    /*  Ref.IntRef의 element 객체를 바꾸는 식으로 바꿈 */
~~~

## inline 함수 
함수를 호출하는 쪽에 함수 본문을 붙여넣게 된다.
-> 미리 변환 할수 있는 영역을 변환시켜놔서 컴파일러 성능을 향상시킴

다른 함수까지 inline을 시키는데, 강제로 인라인을 막을수도 있음

~~~ kotlin
fun main() {
    repeat(2) { println("Hello World") }
}

inline fun repeat(
    times: Int,
    noinline exec: () -> Unit
){
    for( i in 1..times){
        exec()
    }
}
~~~

> non-local-return을 사용할수있게 해줌

~~~ kotlin
fun main() {
    iterate(listOf(1,2,3,4,5)){ num ->
        if( num == 3){
            return // main함수를 리턴함 .. 1,2
        }
        println(num)
    }
}
inline fun iterate(numbers: List<Int>, exec:(Int) -> Unit){
    for(number in numbers){
        exec(number)
    }
}
~~~

inline 함수의 함수 파라미터에서 non-local return을 금지시키는법?
> crossinline


## SAM과 reference

Single Abstract Method 
- 추상 메소드를 하나만 갖고 있는 인터페이스
자바에서는 SAM을 람다로 바꿀수 있음. but koltin에선 불가

~~~ java
@FunctionalInterface
public interface Runnable{
    public abstract void run();
}
~~~

변수명에 넣으려면 SAM 이름 + 람다식을 같이 넣어줘야됨. 근데 파라미터로 바로 쓸거면 상관없음

~~~ kotlin
fun main() {
    val filter: StringFilter = StringFilter { str -> str?.startsWith("A") ?: false }
}
// ...

fun main() {
    consumeFilter({s -> s.startsWith("A")})
}

fun consumeFilter(filter: StringFilter) {}
~~~

기본적으로 SAM 인터페이스로 추정할수 있는 후보가 많을 경우엔, 구체화된 후보로 추정함
아닌 경우, 구체적으로 붙여주면됨

~~~ kotlin
fun main() {
    consumeFilter({s:String -> s.startsWith("A")})
}

fun <T> consumeFilter(filter: Filter<T>) {} 
~~~

kotlin에선 SAM을 이렇게 만들 수 있으나, 1급 시민이여서 굳이 이렇게 만들 필요가 없음
~~~ kotlin
fun main() {
    KStringFilter { it.startsWith("A") }
}

fun interface KStringFilter {
    fun predicate(str: String): Boolean
}
~~~

## reference
Java와 Kotlin의 호출 가능 참조 차이점
Java에선 호출 가능 참조 결과값이 Consumer / Supplier 같은 함수형 인터페이스이지만, 
Kotlin에선 **reflection** 객체이다.



