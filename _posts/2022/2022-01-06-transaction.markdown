---
layout: post
title:  "Connection Pool과 트랜젝션에 대해 알아보자 feat nodejs 2편"
date:   2022-01-07 21:58:00 +0900
categories: dev
---

# 들어가면서
트랜젝션은 사실 백엔드 개발에 있어서 중요하다. 왜냐하면, DB의 4대 특징은 ACID 원자성, 일관성, 독립성, 지속성인데 그 특성이
트랙젝션에 의해 지켜질 수 있기 때문이다. 실제로 JPA를 사용할 때 보면 참조하는 테이블을 lazy fetch 로 조회할 때 @Transactional
선언을 하게 되는 것을 볼수 있는데, 그 이유는 한번의 커넥션으로 두번의 쿼리 질의를 처리하여, 중간에 요청이 들어와도 락을 걸면서 해당 트렌젝션이 정상적으로 끝났을 경우, commit을 비정상적인 경우엔 rollback을 수행한다. 

# 트렌젝션 관련 nodeJS 소스
[참고링크](https://gofnrk.tistory.com/65)

~~~javascript

async function updateMemberName(){

    const conn = await pool.getConnection();

    try{
        await conn.beginTransaction();
    
        const { data } = await conn.query('select * from member where name = ?', ['USER8']);
        // console.log(data[0].name) //user8
        await conn.query('update member set name = ? where name = ?', ['USER8_1',data[0].name]);
        await conn.commit();
    }catch(err){
        console.err('error',err);
        await conn.rollback();
    }finally{
        conn.release();
    }
}

~~~

## 수행결과 
정상적으로 업데이트 쿼리를 수행함을 확인할 수 있다.
![수행 결과](/assets/img/02.png)