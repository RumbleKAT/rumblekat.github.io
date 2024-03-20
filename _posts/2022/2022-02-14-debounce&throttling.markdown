---
layout: post
title:  "Debounce & Throttling" 
date:   2022-02-14 23:40:00 +0900
categories: dev
---

# 들어가면서
Javascript는 단일 스레드이다. 그렇기 때문에 화면에서 보여줄 때 연속해서 많은 작업이 있다면, 화면은 점점 느려지고 사용자는 화면이 멈추는 불편함을 초래한다. 일반적인 DOM 객체의 이벤트 리스너를 수행할때, resize나 input과 같은 이벤트는 조그마한 이벤트 상황에도 계속해서 요청을 수행하는데, 이를 별다른 조치없이 사용한다면 

> 참고자료
~~~
https://www.zerocho.com/category/JavaScript/post/59a8e9cb15ac0000182794fa
https://daniel-park.tistory.com/44
~~~

# Debounce 란?
특정 시간 내 이벤트 발생이 없으면 로직을 수행한다. 

# Throttling 이란?
매 밀리초마다 특정 시간 간격 후에 첫번째 클릭만 즉시 실행한다.
(특정 시간동안의 이벤트는 무시한다. )

## Sample Source 
~~~ html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Debounce & Throttling</title>
</head>
<body>
    <h2>Debouncing </h2>
    <input class="searchBar" />
    <h2>Throttling </h2>
    <input class="searchBar2" />
<script>
    //debouncing 브라우저의 성능 향상을 위함 , 단일스레드여서 자주 사용되는 부분을 줄인다. 0.3초동안 input 이벤트가 발생하지않으면 실행
    var searchBar = document.querySelector('.searchBar');
    var timer;
    searchBar.addEventListener('input',e =>{
        if(timer){
            clearTimeout(timer);
        }
        timer = setTimeout(()=>console.log('ajax call...',e.target.value),300);
    });

    //throttling
    //-> 매 밀리초마다 특정 시간 간격후에 첫번째 클릭만 즉시 실행시킨다. (특정시간동안의  이벤트는 무시한다. 즉, API call cost saving)
    var throttling_timer;
    var searchBar2 = document.querySelector('.searchBar2');
    searchBar2.addEventListener('input',e=>{
        if(!throttling_timer){
            throttling_timer = setTimeout(()=>{
                throttling_timer = null;
                console.log('ajax call.....', e.target.value);
            },300);
        }//기존의 스로틀링 타이머가 존재하면, 아무로직도 실행하지 않는다. 없으면 0.3초 이후 로직을 수행 => 주기적으로 1회 수행한다.
    });    

</script>
</body>
</html>

~~~

![샘플](/assets/img/17.png)

