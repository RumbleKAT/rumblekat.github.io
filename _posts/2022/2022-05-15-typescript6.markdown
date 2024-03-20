---
layout: post
title:  "Typescript #6 기초 문법" 
date:   2022-05-15 21:00:00 +0900
categories: dev
---

# 고차 함수란?
어떤 함수가 또 다른 함수를 반환할 때 그 함수를 고차 함수라고 한다. 함수가 아닌 단순히 값을 반환하는 함수를 **1차 함수** 라고 하고, 1차 함수를 반환하면 2차 고차함수
2차 함수를 반환하면, 3차 고차함수 식으로 표시한다.

~~~ typescript

import { FirstOrderFunc, SecondOrderFunc, ThirdOrderFunc } from "./function-signature";

export const inc:FirstOrderFunc<number,number> = (x:number) :number => x + 1;
export const add:SecondOrderFunc<number,number> = 
    (x:number):FirstOrderFunc<number,number> => (y:number):number => x + y;

console.log(add(4)(5)); //9

export const add3: ThirdOrderFunc<number,number> =
    (x:number): SecondOrderFunc<number,number> =>
    (y:number): FirstOrderFunc<number,number> =>
    (z:number): number => x + y + z;

console.log(add3(2)(4)(5)); //11

~~~

2차 고차 함수를 호출할때, 함수 호출 연산자를 두번 연속해서 사용하는 것을 **커리** 라고 한다. 
고차 함수들은 자신의 차수만큼 함수 호출 연산자를 연달아 사용한다. 만약 자신의 차수보다 함수 호출 연산자를 덜 사용하면, **부분 함수** 라고한다. 

~~~ typescript

const add1: FirstOrderFunc<number, number> = add(1);
console.log(add1(2)) // x + 1이 적용된다. ---> 클로저에 의해서 유효범위가 결정된다.
console.log(add(1)(2)); // x + y가 적용된다.

~~~

# 클로저
고차 함수의 몸통에서 선언되는 변수들은  클로저라는 유효범위를 가진다.
add가 반환하는 함수의 내부범위만 볼때, x는 이해할수 없는 변수이다. 이처럼 범위 안에서는 그 의미를 알수 없는 변수를 **자유 변수** 라고 한다.
타입스크립트는 이처럼 자유 변수가 있으면, 그 변수의 바깥쪽 유효범위에서 자유 변수의 의미를 찾아서 정상 컴파일한다.
이처럼 고차 함수가 부분 함수가 아닌 '값'을 발생해야 비로소 자유 변수의 메모리가 해제되는 유효범위를 클로저라고 한다.
~~~ typescript

function add(x:number):(number) => number{ //바깥쪽 유효범위 시작
    return function(y:number):number{ //안쪽 유효범위 시작
        return x + y; //클로저
    } //안쪽 유효범위 끝
} // 바깥쪽 유효범위 끝

~~~

클로저는 메모리가 해제되지 않고, 프로그램이 끝날 때까지 지속될 수 있다. 
~~~ typescript

const makenames = ():() => string =>{
    const names = ['Jack','Jane','Smith'];
    let index = 0;
    return ():string =>{
        if(index == names.length){
            index = 0;
        }
        return names[index++];
    }
}

const makeName:()=>string = makenames()
console.log([1,2,3,4,5,6].map(n => makeName()));
//makeName 함수를 사용하는 한 makeName 함수에 할당된 클로저는 해제되지 않는다.
~~~

# 함수 조합
함수 조합은 작은 기능(에리티 1)을 구현한 함수를 여러 번 조합해 더 의미있는 함수를 만들어 내는 프로그램 설계 기법이다.
함수  조합을 할수 있는 언어들은 compose 혹은 pipe라는 이름의 함수를 제공하거나 만들 수 있다. 

## Compose
<-- 방향 순으로 함수 수행

~~~ typescript

export const compose = <R>(fn1: (a: R) => R, ...fns: Array<(a: R) => R>) =>
  fns.reduce((prevFn, nextFn) => value => prevFn(nextFn(value)), fn1);

const fn1 = (val: string) => `fn1(${val})`;
const fn2 = (val: string) => `fn2(${val})`;
const fn3 = (val: string) => `fn3(${val})`;

const composedFunction = compose(fn1, fn2, fn3);

console.log(composedFunction("inner")); //fn1(fn2(fn3(inner)))


const first = (x:number) => x+1;
const composed = compose(first,first,first);
console.log(composed(1)); //(((1+1)+1)+1) == 4

~~~

## Pipe
--> 방향 순으로 함수 수행 
~~~ typescript

export const pipe = <R>(fn1: (a: R) => R, ...fns: Array<(a: R) => R>) =>
  fns.reduce((prevFn, nextFn) => value => nextFn(prevFn(value)), fn1);

  const fn1 = (val: string) => `fn1(${val})`;
  const fn2 = (val: string) => `fn2(${val})`;
  const fn3 = (val: string) => `fn3(${val})`;

  const pipedFunction = pipe(fn1, fn2, fn3);

console.log(pipedFunction("1")); //fn3(fn2(fn1(1)))

~~~

# 부분 함수와 함수조합
고차 함수의 부분 함수는 함수 조합에 사용될 수 있다. 

~~~ typescript
export const map = (f:any) => (a:any) => a.map(f);
const square = (value:number) => value * value;
export const squareMap = map(square);

const fourSquare = pipe(squareMap,squareMap);
console.log(fourSquare([3,4])); //[(3*3)*(3*3), (4*4)*(4*4)]

export const reduce = (f:any, initValue:number)=>(a:any)=> a.reduce(f,initValue);
const sum = (result: number, value:number) => result + value;
const sumArray = reduce(sum,0);
console.log(sumArray([1,2,3,4])); // 10

const pitagoras = pipe(
    squareMap,
    sumArray,
    Math.sqrt
)

console.log(pitagoras([3,4])); // 5 -->

~~~

