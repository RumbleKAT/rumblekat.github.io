---
layout: post
title:  "Spring Security #1" 
date:   2022-01-23 20:32:00 +0900
categories: dev
---

# 시큐리티 설정 클래스 작성 
스프링과 달리 부트의 경우엔, 자동 설정 기능이 있어 별도의 설정이 없이도 연동처리가 아래와 같이 가능하다.

![디렉토리설정](/assets/img/11.png)

SecurityConfig 클래스는 시큐리티 관련 기능을 쉽게 설정하기 위해서 WebSecurityConfigurerAdapter 클래스를 상속으로 처리한다. 이는 주로 override를 통해서 여러 설정을 조절한다. 

~~~ java 
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception{
        http.authorizeRequests()
                .antMatchers("/sample/all").permitAll()
                .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
        http.csrf().disable(); //rest 방식으로 이용할수 있도록 보안설정을 다루기 위해 csrf 토큰을 발행하지 않는다.
        http.logout();
        /*
            csrf 토큰 방식을 사용하면, GET 방식으로도 처리 가능
            POST 방식으로만 로그아웃을 해야됨
            invalidatedHttpSession, deleteCookies 를 이용해서 쿠키나 세션을 무효화시키는 설정도 가능
        */
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
        //사용자 계정은 user1
        auth.inMemoryAuthentication()
                .withUser("user1")
                .password(passwordEncoder().encode("1111"))
                .roles("USER");
    }
}

~~~


# 스프링 시큐리티 기본구조 

![스프링 시큐리티 기본구조](https://www.tutorialspoint.com/spring_security/images/components_of_spring_security_architecture.jpg)
https://www.tutorialspoint.com/spring_security/images/components_of_spring_security_architecture.jpg

핵심역할은 Authentication Manager(인증매니저)를 통해서 이루어진다. Authentication Provider는 인증 매니저가 어떻게 동작해야 하는지를 결정하고, 최종적으로 실제 인증은 UserDetailsService에 의해 이루어진다. 

# 인증(Authentication)과 인가(Authorization)
예를 들어, 은행에 금고가 하나 있고, 사용자가 금고의 내용을 열어본다고 가정한다면 아래와 같은 과정을 거친다.
- 사용자는 은행에 가서 자신이 어떤 사람인지 신분증으로 자신을 증명한다.(인증)
- 은행에선 사용자의 신분을 확인한다.
- 은행에서 사용자가 금고를 열어 볼수 있는 사람인지 판단한다.(인가)
- 만일 적절한 권리나 권한이 있는 사용자의 경우, 금고를 열어준다. 

# 필터와 필터 체이닝
필터는 서블릿이나 JSP에서 사용하는 필터와 같은 개념이다. 스프링 시큐리티에서는 스프링의 빈과 연동할 수 있는 구조로 설계되어있다. 

스프링 시큐리티의 내부에는 여러개의 필터가 filter chain 구조로 Request를 처리한다.개발시에 필터를 확장하고, 설정하면 다양한 형태의 로그인처리가 가능하다.
# 인증을 위한 AuthenticationManager
필터의 핵심적인 동작은 AuthenticationManager를 통해서 인증이라는 타입의 객체로 작업을 한다. 전달된 아이디/패스워드로 실제 사용자에 대해서 검증하는 행위는 인증매니저를 통해 이루어진다. 

실제 동작에서 전달되는 파라미터는 UsernamePasswordAuthenticationToken과 같이 토큰이라는 이름으로 전달된다. 즉, 스프링 시큐리티 필터의 주요 역할이 인증 관련된 정보를 토큰이라는 객체로 만들어서 전달. 

AuthenticationManager는 다양한 방식으로 인증처리 방법을 제공. 데이터베이스를 이용할 것인지, 메모리상에 있는 정보를 활용할 것인지 다양한 벙법을 사용 가능.

# 인가와 권한/접근 제한
필터에서 호출하는 AuthenticationManager에는 authenicate()라는 메서드가 있는데 이 메서드의 리턴값은 Authentication이라는 '인증' 정보이다. 이 인증 정보 내에는 Roles라는 '권한'에 대한 정보가 있다. 이 정보로 사용자가 원하는 작업을 할 수 있는지 '허가' 하게되는데, 이러한 행위를 접근 제한 이라고 한다.

1. 사용자가 원하는 URL을 입력한다.
2. 스프링 시큐리티에서는 인증/인가가 필터에서 필요하다고 판단하고, 사용자가 인증하도록 로그인 화면을 보여준다.
3. 정보가 전달된다면, AuthenticationManager가 적절한 AuthenticationProvider를 찾아서 인증을 시도한다.
- AuthenticationProvider의 실제 동작은 UserDetailsService를 구현한 객체로 처리한다. 올바른 사용자로 인증되면 사용자의 정보를 Authentication 타입으로 전달한다. 

# 스프링 시큐리티 커스터마이징
별도의 설정 없이도 기본적으로 스프링 시큐리티는 동작하지만, 개발 시에는 적절하게 인증 방식이나 접근 제한을 지정할 수 있어야한다. SecurityConfig 클래스를 수정하여 처리한다.

## Spring boot 2.0부터 인증을 위해선 반드시 PasswordEncoder를 지정해야한다.
**가장많이 사용하는 것은 BCryptPasswordEncoder**

bcrypt라는 해시함수를 이용하여 패스워드 암호화, 원래대로 복호화가 불가능하고, 매번 암호화된 값도 다르다. 대신 특정한 문자열이 암호화된 결과인지만을 확인할수 있기 때문에,원복 내용을 볼수 없어서 많이 사용된다. @Bean을 이용해서 지정


~~~ java
//SecurityConfig.java
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

//passwordTests

@SpringBootTest
public class PasswordTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testEncode(){
        String password = "1111";
        String enPw = passwordEncoder.encode(password);
        System.out.println("enPw : " + enPw);
        boolean matchResult = passwordEncoder.matches(password, enPw);
        System.out.println("matchResult: " + matchResult);
    }
}
~~~

## AuthenticationManager 설정
SecurityConfig에서는 AuthenticationManager의 설정을 쉽게 처리할 수 있도록 도와주는 configure() 메서드를 override해서 처리한다. 

## Authorization이 필요한 리소스 설정
스프링 시큐리티를 이용해서 특정한 리소스에 접근을 제한하는 방식은 크게 2가지
1. 설정을 통해서 패턴을 지정
2. 어노테이션을 이용해서 적용

패턴을 지정한 예시
~~~ java

    @Override
    protected void configure(HttpSecurity http) throws Exception{
       http.authorizeRequests()
               .antMatchers("/sample/all").permitAll()
               .antMatchers("/sample/member").hasRole("USER");
        http.formLogin(); //인증/인가 처리에서 문제가 발생했을 때, 로그인 페이지를 보이게 지정
        http.csrf().disable(); //rest 방식으로 이용할수 있도록 보안설정을 다루기 위해 csrf 토큰을 발행하지 않는다.
        http.logout();
        /*
            csrf 토큰 방식을 사용하면, GET 방식으로도 처리 가능
            POST 방식으로만 로그아웃을 해야됨
            invalidatedHttpSession, deleteCookies 를 이용해서 쿠키나 세션을 무효화시키는 설정도 가능
        */
    }
~~~

## CSRF 설정
스프링 시큐리티는 기본적으로 CSRF라는 공격을 방어하기 위해 임의의 값을 만들어서 이를 GET 방식을 제외한 모든 요청 방식 등에 포함시켜야만 정상적인 동작이 가능하다. CSRF 공격은 사이트간 요청위조로, 서버에서 받아들이는 요청을 해석하고 처리할때 어떤 출처에서 호출이 진행되었는지 따지지 않기 때문에 생기는 헛점을 노리는 공격방식

CSRF 토큰값을 세션당 하나씩 생성하기 때문에, 경우에 따라선 토큰을 발행하지 않는 경우도 있음.

## logout 설정
별도의 설정이 없는 경우에는 스프링 시큐리티가 제공하는 웹페이지를 제공한다.
CSRF 토큰 사용시 반드시 POST 방식으로만 로그아웃을 처리(이를 해제하면 GET 요청으로도 처리가능)

> Entity 설정

~~~ java

import lombok.*;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ClubMember extends BaseEntity{
    @Id
    private String email;
    private String password;
    private String name;
    private boolean fromSocial;

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ClubMemberRole> roleSet = new HashSet<>();

    public void addMemberRole(ClubMemberRole clubMemberRole){
        roleSet.add(clubMemberRole);
    }
}

~~~

~~~ java

public enum ClubMemberRole {
    USER, MANAGER, ADMIN
}

~~~

> Repository 설정


~~~ java

import org.rumblekat.club.entity.ClubMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember,String> {
    @EntityGraph(attributePaths = {"roleSet"}, type= EntityGraph.EntityGraphType.LOAD)
    @Query("select m from ClubMember m where m.fromSocial = :social and m.email = :email")
    Optional<ClubMember> findByEmail(String email, boolean social);
}

~~~

> Test Data 설정


~~~ java

@SpringBootTest
public class ClubMemberTest {

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void insertDummies(){
        /*
        * 1 ~ 80 User
        * 81 ~ 90 USER, MANAGER
        * 91 ~ 100 USER,MANAGER,ADMIN
        * */

        IntStream.rangeClosed(1,100).forEach(i->{
            ClubMember clubMember = ClubMember.builder()
                    .email("user"+i+"@zerock.org")
                    .name("사용자"+i)
                    .fromSocial(false)
                    .password(passwordEncoder.encode("1111"))
                    .build();
            clubMember.addMemberRole(ClubMemberRole.USER);
            if(i > 80){
                clubMember.addMemberRole(ClubMemberRole.MANAGER);
            }
            if(i>90){
                clubMember.addMemberRole(ClubMemberRole.ADMIN);
            }
            clubMemberRepository.save(clubMember);
        });
    }

    @Test
    public void testRead(){
        Optional<ClubMember> result = clubMemberRepository.findByEmail("user95@zerock.org",false);
        ClubMember clubMember = result.get();
        System.out.println(clubMember);
    }

}

~~~



# 시큐리티를 위한 UserDetailService
로그인 처리는 일반적으로 회원 아이디와 패스워드로 DB를 조회하고, 올바른 데이터가 있다면 세션이나 쿠키로 처리하는 형태로 제작된다. 하지만 스프링 시큐리티는 아래와 같은 다른 특징이 존재한다.

- 스프링 시큐리티에선 회원이나 계정에 대해서 User라는 용어를 사용한다.(User라는 이름 사용시, 겹치지 않게 주의)
- 회원 아이디라는 용어 대신 username이라는 단어 사용(회원을 구별하는 식별 데이터)
- username과 password를 동시에 사용하지 않는다. UserDetailService를 이용해서 회원의 존재만을 우선적으로 가져오고, 이후에 password가 틀리면 잘못된 자격증명이라는 에러를 뱉는다.
- 인증과정이 끝나면, 원하는 자원에 접근할 수 있는 적절한 권한이 있는지를 확인하고 인가 과정을 실행한다.(Access Denied와 같은 결과를 만듬) 

## UserDetails 인터페이스
- getAuthorities() 사용자가 가지는 권한에 대한 정보
- getPassword() 인증을 마무리하기 위한 패스워드 정보
- getUsername() 인증에 필요한 아이디와 정보
- 계정 만료 여부
- 계정 잠김 여부

--> 별도의 클래스를 하나 구성하고 이를 DTO처럼 사용하는 방식으로 제작

![디렉토리 구성](/assets/img/12.png)

> User 기능을 상속받아 ClubAuthMemberDTO를 생성
~~~ java


@Log4j2
@Getter
@Setter
@ToString
public class ClubAuthMemberDTO extends User implements OAuth2User {
    private String email;
    private String password;
    private String name;
    private boolean fromSocial;
    private Map<String,Object> attr;

    public ClubAuthMemberDTO(String username, String password, boolean fromSocial, Collection<? extends GrantedAuthority> authorities, Map<String,Object> attr) {
        this(username, password, fromSocial, authorities);
        this.attr = attr;
    }


    public ClubAuthMemberDTO(String username, String password, boolean fromSocial, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.email = username;
        this.password = password;
        this.fromSocial = fromSocial;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attr;
    }
}

~~~

> UserDetailService 구성

~~~ java
@Log4j2
@Service
@RequiredArgsConstructor
public class ClubUserDetailsService implements UserDetailsService {

    private final ClubMemberRepository clubMemberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("ClubUserDetailsService loadUserByUserName " + username);

        Optional<ClubMember> result = clubMemberRepository.findByEmail(username,false);
        if(!result.isPresent()){
            throw new UsernameNotFoundException("Check Email or Social");
        }

        ClubMember clubMember = result.get();

        log.info("--------------------------------");
        log.info(clubMember);

        ClubAuthMemberDTO clubAuthMember = new ClubAuthMemberDTO(
            clubMember.getEmail(),
            clubMember.getPassword(),
            clubMember.isFromSocial(),
            clubMember.getRoleSet().stream().map(role->new SimpleGrantedAuthority("ROLE_"+role.name())).collect(Collectors.toSet())
        );
        clubAuthMember.setName(clubMember.getName());
        clubAuthMember.setFromSocial(clubMember.isFromSocial());

        return clubAuthMember;
    }
}
/*
* 사용자의 정보를 가져오는 핵심적 역할
* AuthenticationManager는 내부적으로 UserDetailsService를 호출해서 사용자의 정보를 가져온다.
*
* TODO: @RequiredArgsConstructor
* 초기화 되지않은 final 필드나 @NonNull이 붙은 필드에 대해 생성자를 생성한다. -> 주로 의존성 주입 편의성을 위해 사용
*
* */
~~~


