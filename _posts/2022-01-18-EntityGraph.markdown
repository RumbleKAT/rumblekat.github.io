---
layout: post
title:  "@EntityGraph 와 @Modifying"
date:   2022-01-18 22:37:00 +0900
categories: dev
---

# 들어가면서 
JPA에서 @OneToMany 관계일 때, N+1 문제를 해결하기 위해 Lazy로 Fetch를 사용하는 경우가 많다. 그렇지만, 한번에 두가지 테이블 정보를 Join한 정보를 보고 싶을 경우엔, 크게 두가지 방법이 있다. @Query를 이용해서 조인처리를 하거나, @EntityGraph를 이용해서 두 테이블 객체를 로딩하는 방법이 있다.

# @EntityGraph의 속성
- attributePaths : 로딩 설정을 변경하고 싶은 속성의 이름을 배열로 명시한다.
- type : @EntityGraph를 어떤 방식으로 적용할 것인지 설정한다.
- Fetch 속성값은 attributePaths에 명시한 속성은 EAGER로 처리하고, 나머지는 LAZY로 처리한다. 
- Load 속성값은 attributePaths에 명시한 속성은 EAGER로 처리하고, 나머지는 엔티티 클래스에 명시되거나 기본 방식으로 처리한다.

아래 코드를 바꿔서 수행하면, 자동으로 조인처리가 되어 처리된다. (별도의 추가적인 쿼리가 수행되지 않는다.)
일단, 이 방법만으로도 매번 JPQL을 작성할 수고가 덜어진다. 

~~~java

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = {"member"}, type= EntityGraph.EntityGraphType.FETCH) //member attribute에는 Eager로 표현
    List<Review> findByMovie(Movie movie);
}

~~~

# M:N의 관계를 처리시, 트랜젝션 처리에 유의해야한다.
예를 들어, 특정한 회원을 삭제하는경우, 해당 회원이 등록한 모든 영화 리뷰 역시 삭제되어야된다. 그리고, 여기서 중요한 점은 
1) FK를 가지는 리뷰 쪽을 먼저 삭제해야된다.
2) 트랜젝션 관련 처리를 해야한다. (@Transactional , @Commit)

하지만 여기에서도 주의해야하는 점이 있는데, where조건에 member_mid 컬럼을 이용해서, 한번에 3개의 데이터가 삭제될 것 처럼 보이지만
실상은 그렇지 않다.....

~~~ java

    @Transactional
    @Commit
    @Test
    public void testDeleteMember(){
        Long mid = 3L;
        Member member = Member.builder().mid(mid).build();
        reviewRepository.deleteByMember(member); //FK부터 제거한다.
        memberRepository.deleteById(mid);

        /*
        * Hibernate: delete가 3번 호출되는 문제가 있음 -> @Modifying 어노테이션을 사용한다.
    delete
    from
        m_member
    where
        mid=?
        *
        * */
    }

~~~


이러한 비효율을 막기 위해선, @Query를 이용해서 Where절을 지정하는 것이 낫다. update나 delete를 이용하기 위해선 @Modifying 어노테이션이 무조건 필요하다. 

~~~ java
public interface ReviewRepository extends JpaRepository<Review, Long> {
  ...

    @Modifying
    @Query("delete  from Review mr where mr.member = :member")
    void deleteByMember(Member member);

}
~~~
**실행결과**
~~~ SQL

Hibernate: 
    delete 
    from
        review 
    where
        member_mid=?
Hibernate: 
    select
        member0_.mid as mid1_0_0_,
        member0_.moddate as moddate2_0_0_,
        member0_.regdate as regdate3_0_0_,
        member0_.email as email4_0_0_,
        member0_.nickname as nickname5_0_0_,
        member0_.pw as pw6_0_0_ 
    from
        m_member member0_ 
    where
        member0_.mid=?
Hibernate: 
    delete 
    from
        m_member 
    where
        mid=?


~~~