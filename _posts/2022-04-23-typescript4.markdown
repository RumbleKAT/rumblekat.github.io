---
layout: post
title:  "Typescript #4 기초 문법" 
date:   2022-04-23 10:17:00 +0900
categories: dev
---

# 단축 구문
타입스크립트는 매개변수의 이름과 똑같은 이름의 속성을 가진 객체를 만들수 있다. 
이때 속성값 부분을 생략할 수있는 단축 구문을 제공

~~~ typescript
const makePerson = (name:string, age: number)=>{
    const person = {name, age}; //매개변수를 그대로 가져온다.   
}
~~~

# 객체를 반환하는 화살표 함수
객체를 중괄호로 싸고, 소괄호롤 감싸주어야한다.

~~~ typescript
export const makePerson = (name:string, age: number = 10) : Person => ({name , age})
~~~

# 비구조화 할당문 선언하기
함수의 매개변수도 변수의 일종이므로, 비구조 할당문을 적용할 수 있다.

~~~ typescript
export type Person = {name : string, age: number};

const printPerson = ({name, age}: Person) :void=>{
    console.log(`name : ${name}, age: ${age}`);
}

printPerson({name:'Jack',age: 10});
~~~

# 색인 가능 타입(indexable type)
**{[key]: value}** 형태의 타입, 객체 속성의 이름을 변수로 만들려고 할 때 사용한다. 

~~~ typescript
export type KeyValueType = {
    [key: string]: string
}
export const makeObject = (key : string, value:string) :KeyValueType =>({[key]:value})

console.log(makeObject('name','jack')) // {name : 'jack'}
~~~

# 클래스 메서드 구문
타입스크립트에서 메서드는 function으로 만든 함수 표현식을 담고 있는 속성이다. function 키워드로 만든 함수에 this를 사용할 수 있고, 화살표 함수에는 this 키워드를 사용할 수 없다. 

아래 코드가 기본적인 메서드 구현 코드인데, 좀더 타입스크립트처럼 변환하면 다음과 같다.

**변환전**
~~~ typescript
export class A{
    value : number = 1
    method : () => void = (): void =>{
        console.log(`value : ${this.value}`);
    }
}
~~~

타입스크립트는 클래스 속성 중 함수 표현식을 담는 속성은 function 키워드를 생략할수 있게 하는 단축 구문을 제공한다.


**변환후**
~~~ typescript
export class A{
    constructor(public value : number = 1){}
    method() : void{
        console.log(`value : ${this.value}`)
    }
}
~~~

# 메서드 체인
객체의 메서드를 이어서 계속 호출하는 방식의 코드를 작성할수 있다. 이를 구현하려면 메서드가 항상 **this**를 반환하게 해야한다!


~~~ typescript
export class calculator{
    constructor(public value: number = 0){}
    add(value : number){
        this.value += value;
        return this;
    }
    multiply(value : number){
        this.value *= value;
        return this;
    }
}

import { calculator } from "./calculator";

let calc: calculator = new calculator();
let result = calc.add(1).add(2).multiply(4); //왼쪽에서 오른쪽으로 순차실행 
console.log(result); //12
~~~

this를 console.log로 찍어보면 다음과 같다.
~~~ typescript

> C:\Program Files\nodejs\node.exe -r ts-node/register src\app.ts

calculator {value: 1}
calculator {value: 3}
calculator {value: 12}

~~~

# 자바스크립트에서 배열은 객체이다.
JS에서 배열은 다른언어와 다르게 객체이다. 배열은 Array 클래스의 인스턴스인데, 클래스 인스턴스가 객체이기 때문이다. 

# 배열의 타입 
타입스크립트에서 배열의 타입은 '아이템 타입[]'이다.
아래처럼 배열의 내용을 추가할 수 있다.

![샘플](/assets/img/0423/01.png)

# 문자열과 배열 간 변환
타입스크립트에선 문자 타입이 없고, 문자열의 내용 또한 변경할 수 없다. 이런 특징 때문에 문자열을 가공하려면, 먼저 문자열을 배열로 전환해야 한다.

보통 문자열을 배열로 전환할 땐, String 클래스의 split 메서드를 사용하는데, 구분자를 입력받아 string [] 배열로 만들어 준다.

~~~ typescript
split (구분자: string) : string[]
~~~

~~~ typescript
const split = (str: string, delim : string): string [] => str.split(delim);

console.log(split('12345',''));
console.log(split('1,2,3,4,5',','));
~~~

~~~ typescript
join(구분자: string) : string
~~~

~~~ typescript
export const join = (strArray: string[] , delim : string=''):string => strArray.join(delim);
console.log(join(['2','3']));
~~~

# 제네릭 방식 타입
T[] 형태로 배열의 아이템 타입을 한꺼번에 표현하는 것이 편리하다. 

~~~ typescript
export const arrayLength = <T>(array:T[]): number => array.length

export const isEmpty = <T>(array:T[]) :boolean =>
arrayLength<T>(array) == 0;

let numArr = [1,2,3,4];
let strArr = ['1234','222'];

console.log(arrayLength(numArr));
console.log(isEmpty(numArr));

console.log(arrayLength(strArr));

const identity = <T>(n :T) : T => n;
console.log(identity<boolean>(true)) // true 
console.log(identity(true)) // true 자동으로 타입추론을 통해 생략한다. 
~~~

# 전개 연산자
...을 전개 연산자로 쓴다.

~~~ typescript
let arr1 : number[] = [1,2,3];
let arr2 : number[] = [4,5];

let merged : number[] = [...arr1, ...arr2];
console.log(merged); // [1,2,3,4,5]
~~~

# 선언형 프로그래밍과 배열
함수형 프로그래밍은 선언형 프로그래밍과 관련이 있다. 명령형은 CPU 친화적인(성능초점) 방식이고, 선언형은 좀더 인간에게 친화적인 방식이다.

**명령형 프로그래밍 방식**

~~~
for( ; ; ){
    입력데이터 얻기
    입력데이터 가공해 출력데이터 생성
    출력 데이터 생성
}
~~~

**선언형 프로그래밍**

선언형 프로그래밍은 for문을 사용하지 않고, 모든 데이터를 배열로 담는다. 그리고 문제가 해결될때까지 또 다른 형태의 배열로 가공한다.

~~~
문제를 푸는데 필요한 모든 데이터 배열에 저장
입력 데이터 배열을 가공해 출력 데이터 배열 생성
출력 데이터 배열에 담긴 아이템 출력
~~~

**명령형 프로그래밍**
~~~ typescript
let sum = 0;
for(let val = 1;val <=100){
    sum += val;
    val += 1;
}
console.log(sum); //5050
~~~

**선언형 프로그래밍**

함수형 프로그래밍에서 폴드는 특별한 의미가 있다. 폴드는 [1,2,3,...] 형태의 배열 데이터를 가공해 5050과 같은 하나의 값을 생성하려고 할 때 사용한다. 배열의 아이템 타입이 T라고 할때, 배열은 T[]로 표현할수 있는데, **폴드 함수는 T[] 타입 배열을 가공해 T 타입 결과를 만들어 준다.** (즉, 가공 전용 함수)

~~~ typescript
const range = (from : number, to: number):number[] => from < to ? [from, ...range(from+1,to)]: [];
let numbers : number[] = range(1,101);

console.log(numbers);

const fold = <T>(array:T[], callback: (result:T ,val: T) => T,  initValue:T)=>{
    let result :T = initValue;
    for(let i=0;i<array.length;++i){
        const value = array[i];
        result = callback(result,value);
    }
    return result;
}

let result = fold(numbers, (result, value) => result + value, 0);
console.log(result); //5050
~~~

둘은 결과는 같지만, 범용으로 사용되는 함수를 재사용하느냐의 여부 차이가 있다.