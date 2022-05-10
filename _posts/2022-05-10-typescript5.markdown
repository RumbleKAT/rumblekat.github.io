---
layout: post
title:  "Typescript #5 기초 문법" 
date:   2022-05-10 21:26:00 +0900
categories: dev
---

# Iterable`<T>`과 Iterator`<T>` 인터페이스
타입스크립트는 iterator 제공자에 제네릭 인터페이스를 사용할수 있다.

~~~ typescript
export class StringIterable implements Iterable<string>{
    constructor(private strings: string[] = [], private currentIndex: number = 0){}
    [Symbol.iterator]():Iterator<string>{
        const that = this
        let currentIndex = that.currentIndex, length = that.strings.length

        const iterator: Iterator<string> = {
            next(): {value : any, done:boolean}{
                const value = currentIndex < length ? that.strings[currentIndex++]: undefined
                const done = value === undefined
                return  {value, done}
            }
        }
        return iterator
    }
}

for(let value of new StringIterable(['hello','world','!']))
    console.log(value);

> hello
  world
  !
~~~

# Generator 이해하기
ESNext 자바스크립트와 타입스크립트는 yield라는 키워드를 제공한다. yield는 마치 return 키워드처럼 값을 반환한다. 그리고 반드시 function* 키워드를 사용한 함수에서만 사용할 수 있다. function*로 사용한 함수를 Generator라고 한다.

~~~ typescript
export function* generator(){
    console.log('generator started....')
    let value = 1
    while(value < 4){
        yield value++;
    }
    console.log('generator finished....')
}

for(let value of generator())
    console.log(value);

> generator started....
  1
  2
  3
  generator finished....
~~~

# 세미코루틴과 코루틴의 차이
- generator는 세미코루틴이다. 즉, 절반만 코루틴이다.
- 코루틴은 어플리케이션 레벨의 스레드이다. 스레드는 갯수가 제한되어있으므로, 과다하게 AP에서 많이 사용하게 된다면, 운영체제에 무리를 준다. 코루틴의 목적은, 운영체제에 부담을 주지 않으면서 어플리케이션에서 스레드를 마음껏 쓸 수 있게 하는 것이 다.
- 코루틴은 스레드이므로, 일정주기에 따라 자동으로 반복해서 실행된다. 
- 생성기는 절반만 코루틴이므로, 자동으로 수행되진 않는다. 즉, next() 함수가 호출되지 않으면, 곧바로 멈춘다.

**세미코루틴은 타입스크립트처럼 단일 스레드로 동작하는 프로그래밍 언어가 마치 다중 스레드로 동작하는 것처럼 보이게 하는 기능을 한다.**

# yield 키워드
생성기 함수 한에서 yield를 사용할수 있는데, 이는 두가지 기능을 제공한다.

- 반복기를 자동으로 만들어준다.
- 반복기 제공자 역할도 수행한다.

~~~ typescript

export function* rangeGenerator(from:number, to:number){
    let value = from 
    while(value < to){
        yield value++;
    }
}

//while 패턴으로 동작하는 생성기
let iterator = rangeGenerator(1,3+1);
while(true){
    const {value,done} = iterator.next();
    if(done) break;
    console.log(value);
}

//for ..of 패턴으로 동작하는 생성기
for(let value of rangeGenerator(1,3+1))
    console.log(value);
~~~

# yield* 키워드
yield는 단순히 값을 대상으로 동작하지만, yield*는 다른 생성기나 배열을 대상으로 동작한다.

~~~ typescript

function* gen12(){
    yield 1
    yield 2
}

function* gen12345(){
    yield* gen12()
    yield* [3,4] //yield로 들어가면, 배열째로 들어간다. spead가 되지 않음
    yield 5
}

for(let value of gen12345()){
    console.log(value);
}
~~~

# yield 반환값
yield 연산자는 값을 반환한다. 첫줄 외에 모든 줄은 난수가 출력된다. 실행결과는 이전에 next 메서드가 전달한 값이 gen함수의 내부로직에 의해 현재의 value값이 되어 출력된다.

~~~ typescript

export function* gen(){
    let count = 5
    let select = 0;
    while(count--){
        select = yield `you select ${select}`
    }
}

export const random = (max:number, min = 0 ) => Math.round(Math.random() * (max-min)) + min;
 
const iter = gen();
while(true){
    const {value, done} = iter.next(random(10,1));
    if(done) break;
    console.log(value);
}

> you select 0
  you select 4
  you select 9
  you select 6
  you select 5
~~~
