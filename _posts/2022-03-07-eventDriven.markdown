---
layout: post
title:  "Event Driven feat Nodejs" 
date:   2022-03-07 22:20:00 +0900
categories: dev
---

>참고자료
https://www.geeksforgeeks.org/explain-event-driven-programming-in-node-js/
https://velog.io/@moongq/event-driven

# 들어가면서
MSA를 기반으로 서비스를 구성하기 위해서 필요한 것은 Event Driven 아키텍쳐이다. 이는 이벤트를 등록하고, 해당 이벤트에 대한 응답을 리턴하는 방식으로 서비스의 커플링을 줄일 수 있다. 현재 제작중인 토이프로젝트의 경우, 오류 처리, 로깅처리 등을 이벤트 기반으로 만들어 나갈 예정이다.

싱글스레드는 프로세스 내에서 하나의 스레드가 하나의 요청만을 수행하는데, 이때 다른 요청을 동시에 이행할 수 없어서, **싱글스레드 블로킹 모델**이라고 한다.Nodejs는 싱글스레드이지만, 논블로킹 모델로 되어있고, 클러스터링을 통해 프로세스를 포크하여 멀티프로세싱을 이용할수 있다.

# 이벤트 기반이란?
이벤트가 발생할 때 미리 지정한 작업을 수행하는 것을 의미한다. JS에서 addEventListener로 액션을 등록하고, 이벤트가 발생할때, 미리 등록한 콜백함수를 수행하는 식으로 동작한다.

~~~ javascript

dom.addEventListener('click',()=>{
    console.log("콜백 수행");
});

router.get('/getdata',(req,res,next)=>{
    //getdata로 요청이 들어올때 callback 처리 수행한다.
});

~~~

![샘플](/assets/img/24.png)

Nodejs의 내부 구조는 JS와 C++언어로 구성되어있다. 이중 Libuv는 100% C++로 구성된 라이브러리이다. V8엔진에선 JS를 C++로 변환해준다. Nodejs에서 동작하는 이벤트 루프는 libuv 내에서 구현되는데, **Nodejs는 싱글 스레드이기 때문에, 하나의 이벤트 루프를 가지면서, 하나의 스레드가 모든 것을 처리한다.**

## 이벤트 루프의 내부구조
이벤트 루프는 여러개의 페이즈를 가지고 있고, 이는 각자의 Queue를 가진다. 또한, 라운드 로빈 방식으로 노드 프로세스가 종료 될때까지 일정 규칙에 따라 여러개의 페이즈를 계속 순회한다. **FIFO(FIRST IN FIRST OUT)** 순서로 콜백함수를 처리한다. 

## 논블로킹 IO
Nodejs에서는 블로킹 작업들을 백그라운드에서 수행하는데, 실제 백그라운드에서 수행되는 일부 기능은 멀티스레드로도 수행이 된다. **libuv의 스레드 풀은 커널이 지원안하는 작업들을 수행한다.** 즉,libuv의 스레드풀은 멀티스레드이다. 

## 이벤트 루프 내 6개의 페이즈
- timers
이벤트 루프가 페이즈를 순회하면서 timer단계에 오면 처리할수 있는 timer함수들을 확인하고, 콜백함수를 실행한다. 
- pending callback
I/O 작업이 완료되면, I/O 작업 블록 내의 콜백함수들을 poll 단계의 큐로 넘겨준다. 또한, TCP 오류와 같은 시스템 작업의 콜백을 실행한다.
- idle, prepare
- poll
I/O와 연관된 콜백을 제외한 모든 콜백을 수행한다. timer 단계서의 실행시간제어를 담당한다. 
- check 
setImmediate()의 콜백함수가 실행
만약, poll 단계가 유휴상태라면,poll 이벤트를 기다리지 않고 check 단계로 넘어간다. 
- close callback 
close 이벤트에 따른 콜백함수를 실행한다.

아래 예시에서, 만약 이벤트 루프가 돌고있는 시점이 timer라면 setTimeout이 먼저, timer를 지났다면, setImmediate의 콜백함수가 먼저 수행된다.

~~~ javascript

setTimeout(() => {
  console.log('timeout');
}, 0);

setImmediate(() => {
  console.log('immediate');
});

~~~

#Event Driven Programming?
이벤트 기반 프로그래밍은 여러 이벤트의 발생을 동기화하고 프로그램을 최대한 간단하게 만드는데 사용된다. 

이벤트 트리거를 수신하는 함수를 observer라고 한다. 이는 이벤트가 발생할때 트리거가된다. 이러한 이벤트는 '이벤트' 모듈 및 **EventEmitter** 클래스를 통해 접근가능하다.

# 이벤트 기반 프로그래밍 원칙

- 이벤트를 처리하기 위한 기능모음, 구현에 따라 블로킹 혹은 논블로킹이 될 수 있다.
- 등록된 함수를 이벤트에 바인딩한다.
- 등록된 이벤트가 수신되면 이벤트 루프는 새 이벤트를 폴링하고 일치하는 이벤트 핸들러를 호출한다.

~~~ javascript

// Import the 'events' module
const events = require('events');
  
// Instantiate an EventEmitter object
const eventEmitter = new events.EventEmitter();
  
// Handler associated with the event
const connectHandler = function connected() {
    console.log('Connection established.');
  
    // Trigger the corresponding event
    eventEmitter.emit('data_received');
}
  
// Binds the event with handler
eventEmitter.on('connection', connectHandler);
  
// Binds the data received
eventEmitter.on(
    'data_received', function () {
        console.log('Data Transfer Successful.');
    });
  
// Trigger the connection event
eventEmitter.emit('connection');
  
console.log("Finish");

~~~

위 코드에선, connection이라는 이벤트를 발생시키면서, 순차적으로 매핑된 콜백을 수행한다.
