---
layout: post
title:  "Connection Pool과 트랜젝션에 대해 알아보자 feat nodejs 1편"
date:   2022-01-05 22:12:00 +0900
categories: jekyll update
---

# 들어가면서
예전에 과제를 하던 중, 리뷰를 받다가 커넥션 풀 부분과 트랜젝션 부분을 왜 작성하지 않았나라는 피드백을 들은적이있다. 특히 트랜젝션의 경우, 직접 트랜젝션 부분을 콜백형식으로 구현했었는데, 이렇게 구현하면 코드가 지저분해지고 락을 잡는 것이 안되서 트랜젝션 불일치가 일어날 수 있다. 트랜젝션 불일치는 매우 큰 문제이다. 그러므로, 예시를 통해 해당 부분을 다시 짚어보려고 한다.

# Connection Pool이란?
서버에는 요청들을 처리할 수 있는 한도가 있다. 커넥션 풀을 사용하면, DB 연결의 캐시를 사용하여, 추가 요청이 필요할때 빠르게 처리가 가능한 장점이 있다.[위키백과](https://ko.wikipedia.org/wiki/%EC%97%B0%EA%B2%B0_%ED%92%80), 즉 일종의 Thread Pool인 것이다. DB에 접속하는 과정이 가장 부하가 많이 걸리고, Pool이 너무크면 메모리 소모가 크고, 적게하면 대기하는 시간이 오래 걸린다.

사실 이 글을 쓰는 이유는 nodejs 공부를 하다보면, 커넥션 풀과 DB 커넥션을 적절히 사용하는 예시가 잘 없어서 이번 기회에 다뤄보려고 한다. 

# Connection Pool 종류
Apache Commons, Tomcat DBCP,HikariCP 등등
일반적으론 필자의 경우, Spring을 이용할 때 Apache Commons를 이용해서 작성을 했었었다. 

# DB Connection Pool 최적화 방법
성능 향상을 위해선 최대 허용치까지 pool을 할당할수 있지만, 보통은 하드웨어 스펙을 기준을 잡고, 스테이지/ 운영 서버 시점에서 필요한 TPS에 따라 증설을 하는 식으로 작업을 수행한다.  

Connection Pool 사이즈를 알맞게 설정하기 위해서는 아래의 조건을 만족해야한다. [참고링크](https://m.blog.naver.com/PostView.naver?isHttpsRedirect=true&blogId=ssang8417&logNo=221858327113)

- **maxActive >= initialSize**
  (max connection 갯수는 Init 개수와 같거나 크게)
- **maxIdle >= minIdle**
  (Max ide(대기) connection 개수는 Min connection 개수와 같게)
- **maxActive = maxIdle**
  (Max Connection 개수는 Max Idle(대기)와 개수가 같게 설정)

접근하는 사용자가 많을수록 Connection Max 갯수를 설정하는 MaxActive 값이 중요. 특히 이값은 어플리케이션에서 사용하는 Thread Pool 최대 갯수와 어플리케이션 갯수 DB 접근 Process 개수, 하드웨어 등 종합적으로 고려를 해봐야된다.


# 커넥션 풀관련 nodeJS 소스
[참고링크](https://nodeman.tistory.com/9) 
**config.js**
~~~javascript

var config = {
    db_info: {
        connectionLimit: 10,
        waitForConnections: true ,
        host: "127.0.01",
        user: "root",
        password: "0000",
        database: "bootex",
        port: 3306,
        charset: 'utf8',
        multipleStatements: false  // 다중쿼리 허용 X
    }
}
 
module.exports = config;

~~~

**db.js**
~~~javascript

const mysql = require('mysql2/promise');
const { db_info } = require('./config');

const pool = mysql.createPool(db_info);

const db = async(sql , params) =>{
    let result = {};
    try{
        //pool에서 getconnection을 하기 때문에, 속도가 더 빠르다.
        const connection = await pool.getConnection(async conn => conn);
        try{
            const [rows] = await connection.query(sql,params);
            result.data = rows; //실 데이터
            result.state = true;
            connection.release();
        }catch(err){
            console.error('Query error', err);
            result.error = err;
            result.state = false;
        }
    }catch(err){
        console.error('DB error',err);
        result.state = false;
        result.error = err;
    }
    return result;
}

module.exports = db;

~~~

**app.js**
~~~javascript
const db = require('./db');

db('SELECT * from member',[]).then(res=>{
    console.log(res);
});

db('select * from member where name = ?', ['USER8']).then(res=>{
    console.log(res);
});

~~~

## 수행결과 
정상적으로 조회 쿼리를 수행함을 확인할 수 있다.
![수행 결과](./img/01.png)
