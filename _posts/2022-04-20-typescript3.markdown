---
layout: post
title:  "Typescript #3 기초 문법" 
date:   2022-04-20 23:10:00 +0900
categories: dev
---

# 들어가면서
Typescript 기본적인 문법을 알아보고자 한다.

# 익명인터페이스
interface 키워드도 사용하지 않고,  인터페이스의 이름도 없는 인터페이스를 만들 수 있다. 
이를 익명 인터페이스라고 한다. 

~~~ typescript

let ai : {
    name : string,
    age : number,
    etc? : boolean
} = {name : 'jack', age : 32};

console.log(ai);

function printMe(me : {name: string, age:number, etc?:boolean}){
    console.log(me.etc ? `${me.name} ${me.age} ${me.etc}` : `${me.name} ${me.age}`);
}
printMe(ai);

~~~

![샘플](/assets/img/0420/01.jpg)

# 클래스 
접근 제한자로 public,private, protect와 같은 접근 제한자를 이름 앞에 붙일 수 있고, 없으면 public 으로 간주.

# 추상 클래스
추상클래스는 abstract 키워드를 class 앞에 붙여서 만든다. 다른 클래스에서 이 속성이나 메서드를 구현하게 한다.

~~~ typescript
abstract class AbstractPerson5{
    abstract name : string
    constructor (public age? : number){}
}

class Person5 extends AbstractPerson5{
    constructor(public name: string, age? : number){
        super(age);
    }
}

let jack5: Person5 = new Person5('song',10);
console.log(jack5);
console.log(jack5.age);
console.log(jack5.name);

~~~

name 속성 앞에 abstract가 붙었으므로, new 연산자를 적용해 객체를 만들수 없다. 

# static 속성
**클래스 이름.정적 속성 이름** 형태의 점 표기법을 사용해 값을 얻거나 설정

~~~ typescript

Class A{
    static initValue = 1
}

let initVal = A.initValue; //1
~~~

