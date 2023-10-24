---
layout: post
title:  "Kotlin querydsl"
date:   2023-10-24 20:08:00 +0900
categories: dev
---

# 들어가면서
java 프로젝트를 kotlin으로 전환하면서 새로 알게되는 부분을 정리합니다.

## JPQL의 단점
1. 문자열로 쿼리를 작성하기에 버그를 찾기 어렵다.
2. 문법이 조금 달라 그때마다 검색해 찾아보아야 한다.
3. 동적 쿼리 작성이 어렵다.
4. 도메인 코드 변경에 취약하다.
5. 함수 이름 구성에 제약이 있다. (의미있는 이름을 붙이기 어렵다)
> 이런 단점을 보완하기 위해서 **Querydsl**을 함께 사용해야 한다

## 세팅방법
gradle 버전이 상위 버전이여서, querydsl 세팅이 쉬움
~~~ 
    id 'org.jetbrains.kotlin.kapt' version '1.6.21'
    ...
    implementation 'com.querydsl:querydsl-jpa:5.0.0'
    kapt('com.querydsl:querydsl-apt:5.0.0:jpa')
    kapt('org.springframework.boot:spring-boot-configuration-processor')

~~~

위의 코드를 빌드하면, 하기 경로에 객체가 생긴다. 그 뒤로 사용가능
> build/generated/source/kapt/main/com.group.libraryapp.domain.book.QBook

## sample 1
기존 Repository 인터페이스에서 신규 인터페이스를 추가한다. 
- UserRepository
~~~ kotlin
interface UserRepository: JpaRepository<User,Long>, UserRepositoryCustom {
    fun findByName(name: String): User?
    
//    @Query("SELECT u FROM User u LEFT JOIN u.userLoanHistories") 실제 User 안에 넣을때 FETCH가 필요하다.
//    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userLoanHistories")
//    fun findAllWithHistories(): List<User>
}

~~~

- UserRepositoryCustom
~~~ kotlin
interface UserRepositoryCustom {

    fun findAllWithHistories(): List<User>
}
~~~

- UserRepositoryCustomImpl
~~~ kotlin
class UserRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
): UserRepositoryCustom {
    override fun findAllWithHistories(): List<User> {
        return queryFactory.select(user) //select *
            .distinct() //distinct
            .from(user) // from user
            .leftJoin(userLoanHistory).on(userLoanHistory.user.id.eq(user.id)).fetchJoin() // join 앞에 fetch를 fetch join으로 인식한다.
            .fetch()
    }
}
~~~

## sample 2

아예 모듈별 새로운 class를 만든다.

~~~ kotlin

@Component
class UserLoanHistoryQuerydslRepository(
    private val queryFactory: JPAQueryFactory,
){
    fun find(bookName: String, status: UserLoanStatus? = null): UserLoanHistory?{ //default 파라미터로 전달
        return queryFactory.select(userLoanHistory)
            .from(userLoanHistory)
            .where(
                userLoanHistory.bookName.eq(bookName),
                status?.let{
                    userLoanHistory.status.eq(status) //status가 있는 경우에만 조건문을 탄다. 여러 조건으로 들어오면 and로 null이면 무시함
                }
            ).limit(1)
            .fetchOne() //Entity 하나만 리턴
    }

    fun count(status:UserLoanStatus): Long{
        return queryFactory.select(userLoanHistory.count())
            .from(userLoanHistory)
            .where(
                userLoanHistory.status.eq(status)
            )
            .fetchOne() ?: 0L
    }
}
~~~

- BookService

~~~ kotlin

    @Transactional
    fun loanBook(request: BookLoanRequest){
        val book = bookRepository.findByName(request.bookName) ?: fail()
        if(userLoanHistoryQuerydslRepository.find(request.bookName, UserLoanStatus.LOANED)!= null){
            throw IllegalArgumentException("진작 대출되어 있는 책입니다")
        }

        val user = userRepository.findByName(request.userName) ?: fail()
        user.loanBook(book)
    }

    @Transactional(readOnly = true)
    fun countLoanedBook(): Int {
        return userLoanHistoryQuerydslRepository.count(UserLoanStatus.LOANED).toInt()
    }   // select * from userLoanHistory, 전체 데이터 쿼리 메모리 로딩 + SIZE.
        // select count from userLoanHistory where status = LOANED

~~~

