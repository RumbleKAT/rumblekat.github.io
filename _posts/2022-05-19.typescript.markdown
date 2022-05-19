---
layout: post
title:  "Typescript #7 기초 문법" 
date:   2022-05-19 22:06:00 +0900
categories: dev
---

# 람다 라이브러리
람다는 함수형 유틸리티 라이브러리이다. 특징으론 아래와 같다.
- 타입스크립트 언어와 100% 호환
- compose 와 pipe 함수제공
- 자동 커리 기능 제공
- 포인트가 없는 고차 도움 함수 제공
- 조합 논리 함수 일부 제공
- 하스켈 렌즈 라이브러리 기능일부제공
- 자바스크립트 표준 모나드 규격과 호환

# 패키지 구성
~~~ 
> npm init --y
> npm i -D typescript ts-node @types/node
> mkdir src

> npm i -S ramda
> npm i -D @types/ramda

> npm i -S chance
> npm i -D @types/chance
~~~

## tsconfig.json
~~~ json
{
    "compilerOptions" :{
        "module" : "commonjs",
        "esModuleInterop" : true,
        "target" : "es5",
        "moduleResolution" : "node",
        "outDir" : "dist",
        "baseUrl" : ".",
        "sourceMap" : true,
        "downlevelIteration" : true,
        "noImplicitAny" : false,
        "paths" : { "*" : ["node_modules/*"]}
    },
    "include" : ["src/**/*"]
}
~~~

### noImplicitAny가 false인 이유
람다 라이브러리는 자바스크립트를 대상으로 설계, 따라서 타입스크립트는 any타입을 허용해야됨

# Range, Pipe
~~~ typescript

//R.range
const numbers: number [] = R.range(1,1+9);
R.tap(n=>console.log(n))(numbers); //2차 고차함수 현재값을 파악할수 있게 해준다.

//R.pipe 
const dump = <T>(array: T[]) => R.pipe(R.tap(n=>console.log(n)))(array) as T[];
dump(numbers);

~~~

# 자동 커리
람다 라이브러리 함수들은 매개변수를 늘리거나 고차함수로 사용할수 있다. 이를 자동 커리라고 한다.

~~~ typescript
console.log(R.add(1,2)); //3
console.log(R.add(1)(2)); //3
~~~

람다 라이브러리 함수들은 매개변수 개수가 모두 정해져있다. 따라서 가변인수 형태로 구현된 함수는 없다. 만약 이를 **N차 고차 함수** 로 만들고 싶다면, R.curryN 함수를 사용할 수 있다.

~~~ typescript


const sum= (...numbers: number[]):number =>
    numbers.reduce((result:number,sum:number)=> result + sum, 0);

const curriedSum = R.curryN(4,sum); //4차 고차함수를 충족함
console.log(curriedSum(1)(2)(3)); //function
console.log(curriedSum(1)(2)(3)(4)); //

~~~

# 순수함수
람다 라이브러리는 순수 함수를 고려해 설계되었다. 따라서 람다 라이브러리가 제공하는 함수들은 항상 입력 변수의 상태를 변화시키지 않고 새로운 값을 반환한다.
(중간에 변수 오염이 되지 않는다.)
~~~ typescript

const originalArray:number [] = [1,2,3];
const resultArray = R.pipe(R.map(R.add(1)))(originalArray);
console.log(originalArray, resultArray); //[ 1, 2, 3 ] [ 2, 3, 4 ]
~~~

# 배열에 담긴 수 다루기
pipe문 사용시, 디버깅은 R.tap 디버깅 함수를 사용하여 전후의 배열 아이템 값을 화면에 출력한다.

~~~ typescript

import * as R from 'ramda';

//R.range
const numbers: number[] = R.range(1, 9+1);

const incNumbers = R.pipe(
    R.tap(a => console.log('before inc:', a)),  // "strictFunctionTypes": false, 이게 아니면 TS2769: No overload matches this call.   The last overload gave the following error. 오류 발생
    R.map(R.inc),
    R.tap(a => console.log('after inc:', a))
);

const newNumbers: number[] = incNumbers(numbers);
~~~

R.inc = R.add(1)
~~~ typescript
const inc = (b:number):number => R.add(1)(b)
const inc = R.add(1)
R.map((n:number)=>inc(n))
R.map(inc) //콜백함수를 익명 함수로 표현한것, 간결하게 표현가능
~~~

# R.addIndex 함수
Array.map은 두번째 매개변수로 index를 제공하지만, R.map은 index 매개변수를 기본으로 하지 않는다. 따라서 R.map이 Array.map처럼 동작하려면 R.addIndex 함수를 이용해 R.map이 index를 제공하는 새로운 함수로 만들어야된다.

~~~ ts
import * as R from 'ramda';

const addIndex = R.pipe(
    R.addIndex(R.map)(R.add),
    //R.addIndex(R.map)((value: number, index:number)=> R.add(value)(index)),
    R.tap(a => console.log(a))
    /*
    [
        1,  3,  5,  7, 9,
        11, 13, 15, 17
    ] //1+0 2+1 3 + 2....
    */
)

console.log(addIndex(R.range(1,9+1)));
~~~

# R.flip 함수
R.add, R.muliply와 달리 R.subtract, R.divide는 매개변수의 순서에 따라 값이 달라진다. 람다는 R.flip이라는 함수를 제공하는데, 2차 고차 함수의 매개변수 순서를 바꿔준다. A - B => B - A

~~~ typescript

import * as R from 'ramda';

const reverseSubtract = R.flip(R.subtract);

const newArray = R.pipe(
    R.map(reverseSubtract(10)), //value - 10
    R.tap(a=>console.log(a))
)(R.range(1,9+1)) 

/*
[
  -9, -8, -7, -6, -5,
  -4, -3, -2, -1
]
*/
~~~
