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

# 콜백함수
일급함수는 프로그래밍 언어가 제공하는 기능. 함수 표현식이라는 값으로 함수를 매개변수로 받을수 있다.

~~~ typescript
const f = (callback: ()=> void ): void => callback();
~~~

~~~ typescript

export const init = (callback:()=>void):void =>{
    console.log('default initialization finished')
    callback();
    console.log('all finished!')
}

init(()=>{
    console.log("second function executed!");
})

~~~

# 중첩함수
함수형 언어에서 함수는 변수에 담긴 함수 표현식이다. 함수 안에 또 다른 함수를 중첩해서 구현할 수 있다.

~~~ typescript

const calc = (value : number, callback :(param : number) => void):void=>{
    let add = (a : number, b : number) => a + b;
    function multipy(a:number,b:number){
        return a*b;
    }
    let result = multipy(add(1,2),value);
    callback(result);
}
calc(30,(result:number)=> console.log(`result is ${result}`)); // result is 90

~~~

# 고차 함수와 클로저 그리고 부분함수
고차 함수는 또 다른 함수를 반환하는 함수를 말한다. 
고차 함수의 일반적인 형태는 아래와 같다.

~~~ typescript
const add2 =(a:number): (arg0: number) => number =>(b:number):number => a + b;

const result = add2(1)(2);

console.log(result); //3
~~~

위에 내용만 보면 상당히 복잡하다. 이부분을 한단계씩 쪼개서 설명해보겠다.

1. number 타입의 매개변수를 받아 number 타입의 값을 반환하는 함수를 만들어 본다.

~~~ typescript
type NumberToNumberFunc = (arg0: number) => number;
~~~

2. 이 타입을 받는 함수를 만들어본다.

~~~ typescript
export const add = (a:number) : NumberToNumberFunc
~~~
3. add의 반환값을 중첩함수로 구현한다.

~~~ typescript
export const add = (a: number): NumberToNumberFunc => {
   const _add:NumberToNumberFunc = (b:number):number =>{
     return a+b;
   }
   return _add;
};

add(1);
~~~
이 상태에서 add(1) 매개변수로 코드를 수행하면, 중첩함수를 리턴한다. a값은 매개변수로 전달되지만, b값은 아직 안받아서 리턴값이 **함수**로 리턴 된다.
이것을 **부분적용함수**라고 한다.
~~~ 
> ƒ (b) {
        return a + b;
    }
~~~

여기에서 add(1)(2)로 코드를 수행하면, 3을 정상적으로 리턴한다.

a는 add함수의 매개변수이고, b는 _add함수의 매개변수이다. 즉, _add 함수의 관점에서 보면 a는 외부에 선언된 변수이다. 이걸 함수형 프로그래밍 언어에는 **클로저**라고 한다. (JS도 이부분을 적극 사용한다. 특히 this )

JS에서 변환된 모형도 아래와 같이 구현된다. (클로저의 형태)
~~~ javascript

"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.add = void 0;
var add = function (a) {
    var _add = function (b) {
        return a + b;
    };
    return _add;
};
exports.add = add;
console.log((0, exports.add)(1)(2));

~~~

만약 3차 고차함수로 구현되어야 한다면 아래와 같이 함수 호출 연산자가 3개 필요하다.

~~~ typescript
const multiply = (a: number) => (b: number) => (c: number) => a * b * c;
console.log(multiply(1)(2)(3));
~~~

javascript 변환 코드

~~~ javascript
var multiply = function (a) { return function (b) { return function (c) { return a * b * c; }; }; };

console.log(multiply(1)(2)(3));
~~~





