---
layout: post
title:  "Kotlin JPA "
date:   2023-10-19 21:13:00 +0900
categories: dev
---

# 들어가면서
java 프로젝트를 kotlin으로 전환하면서 새로 알게되는 부분을 정리합니다.
Lazy fetching은 리스트와 같은 정보를 조회할때는 유리할 수 있으나, 일부 하위 테이블의 컬럼을 직접 봐야하는 경우엔 장애물이 되기 쉽다.
N+1의 경우가 바로 그러한데, 하나의 객체 안에 리스트로 저장되어 있는 경우엔, 해당 컬럼을 읽을때 하위 컬럼에 가짜 값이 들어간다.

그래서 N+1은 처음 전체 리스트를 받는데 + 1, 리스트의 요소가 N이라 할때 N번의 추가 query를 수행한다.
**이는 성능의 영향을 미친다.**

이를 막기 위해선 Join을 사용해야되는데, **JOIN FETCH**로 해결하거나 **EntityGraph**로 해결한다.

# JOIN FETCH와 단순 JOIN 은 JPA 쿼리에서 다르게 동작한다.

- **JOIN**:
    단순한 JOIN은 연관된 엔티티와의 조인만 수행합니다.
    그러나 JOIN만 사용하는 경우에도 연관된 엔티티는 지연 로딩(Lazy Loading) 방식에 따라 로드됩니다. 즉, 실제로 엔티티에 접근할 때까지 데이터베이스에서 해당 엔티티를 로드하지 않을 수 있습니다.
    JOIN은 주로 조인 조건에 따라 결과를 필터링할 때 사용됩니다.
- **JOIN FETCH**:
    JOIN FETCH는 즉시 로딩(Eager Loading)을 강제하는 방법입니다. 이를 사용하면 조인된 엔티티를 즉시 데이터베이스에서 로드합니다.
    이 방식을 사용하면 N+1 문제를 피할 수 있습니다.

> 따라서, JOIN만 사용하고 FETCH를 생략하면, 연관된 엔티티는 쿼리의 일부로 조인되지만, 그 엔티티의 로딩 전략은 **Lazy Loading**에 따라 지연 로딩될 수 있습니다.

~~~ kotlin

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository: JpaRepository<User,Long> {
    fun findByName(name: String): User?
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userLoanHistories")
    fun findAllWithHistories(): List<User>
}

~~~

# DTO의 Companion 함수를 적절히 이용한다.
Kotlin에서 companion object는 자바의 static 키워드에 대응하는 역할을 합니다. 그러나 Kotlin에는 static 키워드가 없기 때문에 companion object를 사용하여 같은 효과를 얻습니다.

## Companion 함수의 주요역할과 특징
- Static-Like 기능: companion object 내부의 멤버나 함수는 클래스 이름을 통해 직접 접근할 수 있습니다. 즉, 인스턴스를 생성하지 않고도 해당 함수나 변수에 접근할 수 있습니다.

- Singleton 인스턴스: companion object는 해당 클래스의 싱글턴 인스턴스로 동작합니다. 따라서 여러번 호출되어도 항상 동일한 인스턴스를 반환합니다.

- Interface 구현: companion object는 인터페이스를 구현할 수 있습니다. 이를 통해 특정 인터페이스를 구현하는 싱글턴 객체를 생성할 수 있습니다.

- 확장 함수: companion object는 확장 함수를 가질 수 있습니다. 이를 통해 기존 클래스에 메서드를 추가하지 않고도 새로운 함수를 정의할 수 있습니다.

~~~ kotlin

data class UserLoanHistoryResponse(
    val name: String, //유저 이름
    val books: List<BookHistoryResponse>
){
    companion object {
        fun of(user: User): UserLoanHistoryResponse{
            return UserLoanHistoryResponse(
                name = user.name,
                books = user.userLoanHistories.map(BookHistoryResponse::of)
            )
        }
    }
}

data class BookHistoryResponse(
    val name: String, // 책의이름
    val isReturn: Boolean,
){
    companion object {
        fun of(history:UserLoanHistory):BookHistoryResponse{
            return BookHistoryResponse(
                name = history.bookName,
                isReturn = history.isReturn,
            )
        }
    }
}

    @Transactional(readOnly = true)
    fun getUserLoanHistories() : List<UserLoanHistoryResponse> {
        return userRepository.findAllWithHistories()
            .map(UserLoanHistoryResponse::of)
    }


~~~

## ?:(엘비스 연산자) 을 잘 써보자
자바를 쓰다보면 가장 소스가 길어지는 부분이 try catch 오류를 잡는 부분이 그중 하나인 것 같다. 엘비스 연산자는 좌측 피연산자의 값이 null이 아니면 좌측 피연산자의 값을 반환하고, null이면 우측 피연산자의 값을 반환합니다. 다시 말해, 좌측 피연산자의 값이 null일 때 기본 값을 제공하는 데 사용된다.

>> 엘비스 연산자를 잘쓰면 에러 처리 소스를 간결화 할수 있다.

~~~ kotlin

    //UserService.kt
    @Transactional
    fun deleteUser(name:String){
        val user = userRepository.findByName(name) ?: fail()
        userRepository.delete(user)
    }


    //ExceptionUtils.kt
    fun fail(): Nothing{
        throw IllegalArgumentException()
    }
~~~