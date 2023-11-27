---
layout: post
title:  "Kotlin Generic #1"
date:   2023-11-23 21:44:00 +0900
categories: dev
---

# 들어가면서
Kotlin을 공부하면서 새로 알게되는 부분을 정리합니다.

~~~ kotlin
package function

abstract class Animal(
  val name: String,
)

abstract class Fish(name: String): Animal(name)

class GoldFish(name: String): Fish(name)
class Carp(name: String): Fish(name)

class Cage{
  private val animals: MutableList<Animal> = mutableListOf()

  fun getFirst(): Animal{
    return animals.first()
  }

  fun put(animal: Animal){
    this.animals.add(animal);
  }

  fun moveFrom(cage: Cage){
    this.animals.addAll(cage.animals);
  }

}

class Cage2<T>{
  private val animals: MutableList<T> = mutableListOf()

  fun getFirst(): T{
    return animals.first()
  }

  fun put(animal: T){
    this.animals.add(animal);
  }

  fun moveFrom(cage: Cage2<T>){
    this.animals.addAll(cage.animals);
  }
}

fun main(){
  val cage = Cage()
  cage.put(Carp("잉어"))

  // 1. val carp: Carp = cage.getFirst() as Carp // 타입케스팅을 하는데, 다른 객체를 넣었을땐 runtime error가 발생함
  // 2. Safe Type Casting과 Elvis Operator
  // 3. Generic

  val cage2 = Cage2<Carp>()
  cage2.put(Carp("잉어"))
  val carp = cage2.getFirst()

  val goldFish = Cage2<GoldFish>()
  goldFish.put(GoldFish("금붕어"))

  val cage3 = Cage2<Fish>()
  cage.moveFrom(goldFish) // Type MisMatch
}
~~~


## 상위 타입과 하위 타입의 의미
- Int -> Number
- 상위 타입이 들어가는 자리에 하위타입이 들어갈수있다.

Cage2<Fish> 와 Cage2<GoldFish>의 상속관계는 무공변하다.
즉, 제네릭 클래스는 타입 파라미터간의 상속관계를 유지하지 않는다.

JAVA의 배열은 제네릭과 다르다.
A 객체가 B객체의 하위 타입이면 상위 타입으로 간주
상속관계가 그대로 간다.

Java의 배열은 이런 코드가 가능하다.

~~~ java
String [] strs = new String[] {"A","B","C"};
Object[] objs = strs;
objs[0] = 1; // 런타임때 오류가 난다.
~~~

즉, 자바도 타입 안전하지 않는 코드가 작성될수 있다.

List는 제네릭을 사용하기 때문에 무공변하다.

**배열보다는 리스트를 사용하는 이유.**
~~~ java
List<String> strs = List.of("A","B","C")
List<Object> objs = strs; // Type Mismatch!
~~~

상속관계가 Fish와 GoldFish가 상속관계가 유지되게 하면 컴파일 오류를 해결할 수 있다.

## out을 써서 변성을 주다. 
out을 붙이게 되면, otherCage로 부터 꺼내게 된다.
out을 넣으면 생산자 역할만 할수있고, getAnimals(), getFirst() 함수만 호출할 수 있다.

타입 안정성이 깨질수 있기 때문에, 생산자 역할만 한다.

~~~ kotlin

fun main() {
  val cage: Cage2<out Fish> = Cage2<GoldFish>()
}

class Cage2<T : Any> {
  private val animals: MutableList<T> = mutableListOf()

  fun getFirst(): T {
    return animals.first()
  }

  fun put(animal: T) {
    this.animals.add(animal)
  }

  fun moveFrom(otherCage: Cage2<out T>) {
    this.animals.addAll(otherCage.animals)
  }
}
~~~

## in을 쓰는 케이스, 반 공변하다.
in은 소비자

~~~ kotlin 

fun main() {
  val fishCage = Cage2<Fish>()

  val goldFishCage = Cage2<GoldFish>()
  goldFishCage.put(GoldFish("금붕어"))
  goldFishCage.moveTo(fishCage)
}

~~~

금붕어를 Fish Cage로 옮기는 것도 Type mismatch가 발생한다.

~~~ kotlin
  fun moveTo(otherCage: Cage2<in T>) {
    otherCage.animals.addAll(this.animals)
  }
~~~


## 제네릭은 JDK 초기 버전부터 있던게 아니다.

자바 5부터 List<String>도 런타임 때는 타입 정보를 제거함
자바에선 지금도 raw type을 만들 수 있다. 그러나, 코틀린은 언어 초기부터 제네릭이 고려됨.

런타임 때는 타입정보가 사라진다. -> 타입 소거

star projection을 활용해서 최소한 List인지는 확인할 수있다.
해당 타입 파라미터에 어떤 값이 들어있는지는 모른다. 그러나 리스트는 확실함

~~~ kotlin
if( data is List<*>){
  val element: Any? = data[0]
}
~~~

mutableList에선 함부로 데이터를 넣을 수는 없다.
제네릭 함수에서도 타입정보는 사라진다.

주어진 리스트에 T 타입의 정보를 알아야하는 상황 일때
**inline** 함수와 **reified** 지시어를 사용해서 처리한다.

~~~ kotlin
fun main() {
  val numbers = listOf(1,2f,3)
  println(numbers.filterIsInstance<Float>())
}

inline fun <reified T> List<*>.hasAnyInstanceOf() : Boolean{
  return this.any {it is T}
} // T의 인스턴스를 만들거나 T의 companion을 쓸순없음

~~~







