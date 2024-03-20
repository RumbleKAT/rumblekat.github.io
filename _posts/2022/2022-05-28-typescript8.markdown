---
layout: post
title:  "Typescript #8 기초 문법" 
date:   2022-05-28 16:06:00 +0900
categories: dev
---

# 서술자와 조건연산
Array.filter 함수에서 사용되는 콜백함수는 boolean 값을 반환해야하는데, 함수형 프로그래밍에서 boolean 값을 반환해 어떤 조건을 만족하는지를 판단하는 함수를 '서술자'라고한다.
~~~ typescript
R.lt(a)(b):boolean // a<b이면 true
R.lte(a)(b):boolean //a<=b이면 true
R.gt(a)(b):boolean // a>b
R.gte(a)(b):boolean // a>=b  

import * as R from 'ramda';

R.pipe(
    R.filter(R.lte(3)),
    R.tap(n=>console.log(n))
)(R.range(1,10+1));

/*
[
  3, 4, 5,  6,
  7, 8, 9, 10
]
*/
~~~
boolean 타입의 값을 반환하는 함수들은 R.allPass와 R.anyPass라는 로직 함수를 통해 결합할수있다. 여러 조건을 동시에 넣을수있다.
~~~ typescript
import * as R from 'ramda';

type NumberToBooleanFunc = (n: number) => boolean;
export const selectRange = (min:number, max:number) : NumberToBooleanFunc =>
R.allPass([
    R.lte(min),
    R.gt(max)
]);

R.pipe(
    R.filter(selectRange(3,6+1)),
    R.tap(n=>console.log(n))
)(R.range(1,10+1)); //3에서 7까지 숫자 리턴
~~~

R.not은 입력값이 true이면 false, false이면 true를 반환한다.
~~~ typescript
import * as R from 'ramda';

type NumberToBooleanFunc = (n: number) => boolean;
export const selectRange = (min:number, max:number) : NumberToBooleanFunc =>
R.allPass([
    R.lte(min),
    R.gt(max),
]);

const notRange = (min: number, max : number) => R.pipe(selectRange(min,max),R.not);

R.pipe(
    R.filter(notRange(3,6+1)),
    R.tap(n=>console.log(n))
)(R.range(1,10+1));
//[ 1, 2, 7, 8, 9, 10 ]
~~~

R.ifElse 함수는 세가지 매개변수를 포함하는데, 첫번째는 true/false를 반환하는 서술자를, 두번째는 선택자가 true반환할때 실행할 함수, 세번째는 false를 반환할때 실행할 함수를 리턴한다.
~~~ typescript
import * as R from 'ramda';

const input: number [] = R.range(1,10+1), halfValue = input[input.length/2];
//6

const substractOrAdd = R.pipe(
    R.map(R.ifElse
        (R.lte(halfValue), //half <= x
        R.inc, 
        R.dec)),
        R.tap(n => console.log(n))
)
substractOrAdd(input)
//6보다 작으면 1씩감소, 크면 1씩 증가
~~~

# 문자열 다루기
R.trim은 문자열 앞뒤의 공백을 제거한다.
~~~ typescript
import * as R from 'ramda';

console.log(R.trim('\t hello \n'));
~~~

## 대소문자 전환
R.toLower 함수는 문자열에서 대문자를 모두 소문자로 전환해주며, R.toUpper는 반대로 소문자를 모두 대문자로 전환
~~~ typescript
import * as R from 'ramda';

console.log(
    R.toUpper('Hello'), //HELLO
    R.toLower('HELLO') //hello
);
~~~

## 구분자를 사용해 문자열을 배열로 변환
R.split 함수는 구분자를 사용해 문자열을 배열로 바꿔준다.
R.join 함수는 문자열로 바꿔준다.

~~~ typescript
const words: string[] = R.split(' ')(`Hello world!, I'm Peter.`);
console.log(words); //[ 'Hello', 'world!,', "I'm", 'Peter.' ]
~~~

## toCamelCase 함수 만들기

타입스크립트에서 문자열은 readonly 형태로만 사용할수 있다. 따라서 일단 문자열을 배열로 전환해야한다. 문자열의 앞뒤 공백을 제거하고 delim 매개변수로 전달받은 문자열을 구분자로 삼아 배열로 전환후 모두 소문자로 변환한다. 그리고 두번째 문자열부터 첫 문자만 대문자로 변경한다. 그리고 다시 문자열로 전환한다.

~~~ typescript
import * as R from 'ramda';

type StringToStringFunc = (string:string) => string
export const toCamelCase = (delim:string):StringToStringFunc => {
    const makeFirstToCamel = (word:string)=>{
        const characters = word.split('')
        return characters.map((c,index)=> index == 0 ? c.toUpperCase():c).join('')
    }
    
    const indexedMap = R.addIndex(R.map);
    return R.pipe(
        R.trim,
        R.split(delim),
        R.map(R.toLower),
        indexedMap((value:any,index:any)=> index > 0 ? makeFirstToCamel(value) : value
        ),R.join('')
    ) as StringToStringFunc
}

console.log(toCamelCase(' ')('Hello world'));
//helloWorld
~~~





