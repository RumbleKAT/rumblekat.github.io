---
layout: post
title:  "Kotlin test"
date:   2023-10-25 20:26:00 +0900
categories: dev
---

# 들어가면서
java 프로젝트를 kotlin으로 전환하면서 새로 알게되는 부분을 정리합니다.

테스트 코드 작성할때, 영속성 컨텍스트 접근해야되는 케이스의 경우, 처리방식이 여러가지가 있다.

1. @Transactional을 붙이는 것이다.

하지만, 만약 테스트엔 @Transactional이 있고, 본 코드에 빼먹는 경우, 이는 오류로 이어진다.

2. JPQL이나 QueryDSL로 fetch join한 결과를 조회

여러 테이블과의 JOIN 가능: 당연히 연관된 여러 엔터티나 컬렉션과 함께 FETCH JOIN을 사용할 수 있습니다. 그러나 실제로 여러 FETCH JOIN을 함께 사용하면 성능 문제나, 데이터 중복 등의 문제가 발생할 수 있습니다.

카테시안 곱: 두 개 이상의 컬렉션에 대한 FETCH JOIN을 사용하면 반환되는 결과의 수가 두 컬렉션의 크기의 곱이 됩니다. 예를 들어, 하나의 엔터티가 10개의 아이템 컬렉션과 10개의 이벤트 컬렉션을 가지고 있으면, 두 컬렉션 모두에 FETCH JOIN을 사용하면 반환되는 결과의 수는 100이 됩니다.

실제 사용 시 주의: 실제 애플리케이션에서 FETCH JOIN을 사용할 때는 성능 문제나 예상치 못한 결과가 나올 수 있으므로 주의가 필요합니다. 특히 FETCH JOIN을 사용하는 쿼리가 여러 곳에서 사용될 경우, 어떤 경우에는 필요하지 않은 데이터까지 불러오게 될 수 있습니다.

3. 트렌젝션 람다를 이용한 방법이다.

~~~ kotlin

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TxHelper {

    @Transactional
    fun exec(block: () -> Unit){
        block()
    }
}

~~~

~~~ kotlin

@SpringBootTest
class TempTest @Autowired constructor(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val txHelper: TxHelper,
){

    @Test
    fun test(){
        //when
        userService.saveUserAndLoanTwoBooks()

        txHelper.exec {
            //then 미리 선언한 람다로 처리
            val users = userRepository.findAllWithHistories()
            assertThat(users).hasSize(1)
            assertThat(users[0].userLoanHistories).hasSize(2) // 테스트코드가 지연초기화를 할수없음
        }
    }
}

~~~