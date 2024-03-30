---
layout: post
title:  "AWS Lambda with SpringBoot and SnapStart"
date:   2024-03-30 18:35:00 +0900
categories: dev
---

# 들어가면서
> https://aws.amazon.com/ko/blogs/korea/new-accelerate-your-lambda-functions-with-lambda-snapstart/
> https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtime-environment.html#runtimes-lifecycle
> https://www.lgcns.com/blog/cns-tech/aws-ambassador/48783/
> https://www.lgcns.com/blog/cns-tech/aws-ambassador/49072/
> https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring-Boot3
> https://medium.com/@jmmoon1974/lambda%EC%9D%98-concurrent-execution%EC%97%90-%EB%8C%80%ED%95%B4%EC%84%9C-%EC%9E%98-%EB%AA%BB-%EC%9D%B4%ED%95%B4%ED%95%98%EB%8A%94-%EB%B6%84%EB%93%A4%EC%9D%B4-%EC%9E%88%EC%96%B4-%EA%B8%80%EC%9D%84-%EC%9E%91%EC%84%B1%ED%95%B4-%EB%B4%85%EB%8B%88%EB%8B%A4-34913a7821f2

AWS람다는 개발 측면에서, 프로그래밍이 간단하고, 운영측면에서는 변화하는 사용 패턴에 신속하게 대응 할수 있는 어플리케이션이다.
람다의 특징은 **함수가 안전하고, 격리된 환경**에서 실행된다는 것이다. 각 환경의 생명주기는 **Init**, **Invoke**, **Shutdown** 이라는 3가지 단계로 구성된다.

## AWS 람다의 생명주기 별 특징

| Category   | Desciption                                                                   |
|------------|------------------------------------------------------------------------------|
| Init       | 함수의 런타임을 부트스트랩하고, 정적 코드를 실행  (INIT_REPORT)                  |
| Invoke     | API 요청에 대한 응답으로 Lambda 함수가 호출                                     |
| Shutdown   | 런타임을 종료. 지속시간은 2초로 제한되며, 만약 응답이 없으면 Shutdown SIGKILL     |

![lambda_lifecycle](https://docs.aws.amazon.com/images/lambda/latest/dg/images/Overview-Successful-Invokes.png)

## Init 단계 
- 모든 확장 프로그램 초기화(ex. datadog, Splunk, log insight...)
- 런타임을 부트스트랩
- 함수의 정적 코드 초기화
- 런타임 후크 실행(Lambda SnapStart만 해당) beforeCheckpoint

런타임 및 모든 확장이 API 요청을 전송하여 준비가 되었음을 알리면 단계가 종료된다. 단계는 10초로 제한된다. 세 가지 작업이 모두 수행되지 않는 경우 **10초 이내**에 완료되면 Lambda는 첫 번째 함수 시점에 단계를 재시도한다. 대부분의 경우, 몇 밀리초 내에 완료되지만, 여러가지 이유로 상당한 시간이 걸릴 수 있다.

예를 들어, Spring Boot, Quarkus 또는 Micronaut와 같은 프레임워크와 함께 Java 런타임 중 하나를 사용하는 Lambda 함수의 Init 단계는 때때로 10초까지 걸릴 수 있다.(종속성 삽입, 함수 코드 컴파일 및 클래스 패스 구성 요소 스캔 포함).

## Cold Start란?
AWS Lambda는 요청이 발생하는 시점에 인스턴스를 가동하고 코드를 실행한다.
코드를 실행하기 위해서 위의 Init 단계가 선행되어야되는데, 이를 **ColdStart**라고 한다.
첫번째 요청은 ColdStart로 인해 많은 시간이 소요되나, 두번째 부턴 동일한 컨테이너에 코드 실행만 하면되서 빠르게 처리 가능하다.

![lambda_coldstart](https://www.lgcns.com/wp-content/uploads/2023/10/1-2.png)

## Cold Start가 발생하는 요인
- 함수에 대한 첫번째 요청 발생시
- 새 어플리케이션 버전 배포시
- 일정 시간 동안 요청이 없을 경우 인스턴스가 정리되고, 새로운 인스턴스가 실행될 경우
- 동시 호출 발생하여 가용한 인스턴스가 없을 경우 (**Concurrency**에서 좀더 서술)

## Lambda Snapstart의 특징
-  특정 Lambda 함수에 대해 Lambda SnapStart를 활성화한 후 새 버전의 함수를 게시하면 최적화 프로세스가 트리거된다.
- 프로세스에 의해 함수가 시작되고 Init 단계 전체에 걸쳐 프로세스가 실행된다.
- 메모리 및 디스크 상태의 변경 불가능한 암호화된 스냅샷을 가져와서 다시 사용할 수 있도록 캐시된다. 
- 함수가 호출되면 필요에 따라 상태가 캐시에서 청크 단위로 검색되어 실행 환경을 채우는 데 사용된다.
- 추가요금 안 붙는다.

결론적으로, 새로운 실행 환경을 만드는 데, 더 이상 전용 Init 단계가 필요하지 않으므로 이 최적화를 통해 호출 시간이 단축되고 더 잘 예측할 수 있게 된다.

SnapStart 기능을 쓰면 2가지 Phase로 나뉘어서 작동을 한다.
- Deployment phase
    - 배포하는 동안 Lambda는 런타임을 생성하고, 새 코드로 Init 작업을 수행
    - 초기화가 완료되면 메모리와 로컬 디스크 상태를 스냅샷으로 생성한다.
    - Lambda 버전 배포시 alias를 지정하여 배포하며, 배포 완료 전까지는 이전 버전이 트리거 된다.
- Invocation phase
    - ColdStart 3단계 대신, Restore 단계가 수행되며, Snapstart를 로딩하여 초기의 상태로 빠르게 복원한다.
    - Warm Start 조건에선 Invoke Stage만 진행된다.

![snapstart_phase](https://www.lgcns.com/wp-content/uploads/2023/10/2-2.png)

## snapstart alias

![snapstart_alias](https://www.lgcns.com/wp-content/uploads/2023/10/3.1.png)

## Snapstart 주의할점
- 고유성: 난수 데이터의 Seeding에 대한 문제가 있다. 고유성을 유지해야 되는 값이 스냅샷에 포함된 경우 고유성을 유지할 수 없다.예를 들면, Bootstrap 단계에서 생성되는 난수가 global 변수로 저장될 경우 해당 변수는 스냅샷에 포함이 됩니다. 모든 Lambda 함수는 동일한 값을 사용하게 되어 고유성을 잃게 됩니다.
이를 해결하기 위해서 고유성을 유지해야 되는 데이터를 handler function에서 생성하거나, 런타임 hook을 이용하여 처리해야 됩니다.
- 캐싱 지속기간: 14일 동안 사용하지 않으면 제거. 업데이트되거나 패치된 런타임에 따라 스냅샷이 달라지는 경우 Lambda는 자동으로 캐시를 새로 고칩니다.
- 지연시간에 매우 민감한 함수는 Provisioned Concurrency를 쓰는것이 나을수도 있다.
- Corretto JAVA 11 버전 이후 사용 가능.
- 배포 시간 증가

## Hook을 이용한 Init 시간 줄이기
OpenSource CRaC Project의 일환으로 런타임 Hook을 제공.

1. BeforeCheckPoint
스냅샷을 생성하기 이전인, 코드 초기화 단계에 실행되는 hook이다.
Spring Handler 전역 변수 구성한 것과 비슷한 효과를 볼 수 있다. 예를 들어, DB 조회한 대량의 메모리를 Caching해서, beforeCheckpoint Method를 통해 객체를 생성해서 데이터를 담고 나중에 호출할 수 있는 형태로 구성 가능. 그러나 오래된 데이터 참조 가능성은 존재.

2. AfterRestore
AfterRestore는 스냅샷을 Restore 한 후에 실행되는 hook이다.
10초의 Timeout이 있다. DB의 Connection Pool 생성과 같이 invoke 단계 이전에 수행해야 되지만, 스냅샷보다는 최신의 상태를 유지해야 되는 경우에 사용할 수 있다.
그러나, afterRestore hook은 Cold Start 시간을 늘어나게 하므로 사용 시 주의가 필요하다.

## 사내 인증시스템 적용기
사내 통합인증 서버를 AWS Lambda와 Snapstart를 기반으로 만들었고, 그 결과는 아주 성공적이었다. 해당 프로젝트의 주 목적은 이러했다.

- 기존에 Custom Authorizer 람다 기반의 인증 체계에서 통합 인증 토큰을 활용한 인증속도 개선
- 3rd Party 인증 후 Cognito SAML 인증 방식으로 이어지는 부분의 사용성 저하 문제
- 신규 인증 방식의 추가 니즈
- OpenAPI나 리전간 통신에 대한 인증 방식 필요
- 장애에 복구성이 높고, 증가하는 TPS에 반응적으로 대응 필요

![인증서버_기능](/assets/img/2024/240330/auth_functional.png)

사내 인증서버의 구성은 다음과 같다.

- Spring Boot 3
- JAVA 17
- Spring Security 
- JDBCTemplate 
- Jedis
- aws-serverless-java-container-springboot3
    - 이 라이브러리는 spring cloud function에 기초한다. 
    - https://github.com/RumbleKAT/aws-spring-cloud-function

SnapStart를 수행하지 않고 배포를 했을땐 1분 안쪽으로 배포가 된다. 그렇지만, SnapStart를 수행하는 경우 3분정도 소요가 된다. 추가로, 환경 변수의 이름을 변경하거나 할땐 CloudFormation이 Crashed 되는 경우가 있어서 몇번 Stack 삭제를 진행했었다. 환경변수 편집이 필요하다면 Snapstart를 제외하고 다시 배포하면 정상적으로 반영된다. 

## Snapstart를 위한 성능 개선

- static으로 SpringBootLambdaContainerHandler 수행
    후속 요청에도 인스턴스를 재사용하므로, 성능을 개선할수 있다.
    ~~~ java
        public class StreamLambdaHandler implements RequestStreamHandler {
        private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
        static {
            try {
                handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
                // If you are using HTTP APIs with the version 2.0 of the proxy model, use the getHttpApiV2ProxyHandler
                // method: handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(Application.class);
            } catch (ContainerInitializationException e) {
                // if we fail here. We re-throw the exception to force another cold start
                e.printStackTrace();
                throw new RuntimeException("Could not initialize Spring Boot application", e);
            }
        }

        @Override
        public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
                throws IOException {
            handler.proxyStream(inputStream, outputStream, context);
        }
    }
    ~~~
- Tomcat 제거 
    기본적으로 Springboot 프로젝트엔 tomcat 서버가 포함되므로, shade 플러그인으로 제거한다.
    ~~~ xml
    <build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <configuration>
                <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <artifactSet>
                            <excludes>
                                <exclude>org.apache.tomcat.embed:*</exclude>
                            </excludes>
                        </artifactSet>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
    ~~~
- 비동기 초기화
    비동기 이니셜라이저는 JVM 시작 시간을 검색하여 AWS Lambda가 핸들러를 생성한 이후 이미 경과된 시간을 추정한다. 기본적으로 Serverless Java Container는 추가로 10초 동안 대기하지만, 이를 통해 좀더 앞당길 수 있다.
    ~~~ java
    import com.amazonaws.services.lambda.runtime.Context
    import com.amazonaws.services.lambda.runtime.LambdaLogger
    import com.amazonaws.services.lambda.runtime.RequestStreamHandler
    ...
    // Handler value: example.HandlerStream
    public class HandlerStream implements RequestStreamHandler {
        @Override
        /*
        * Takes an InputStream and an OutputStream. Reads from the InputStream,
        * and copies all characters to the OutputStream.
        */
        public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
        {
            LambdaLogger logger = context.getLogger();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("US-ASCII")));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("US-ASCII"))));
            int nextChar;
            try {
            while ((nextChar = reader.read()) != -1) {
                outputStream.write(nextChar);
            }
            } catch (IOException e) {
            e.printStackTrace();
            } finally {
            reader.close();
            String finalString = writer.toString();
            logger.log("Final string result: " + finalString);
            writer.close();
            }
        }
    }
    ~~~ 
- @ControllerAdvice
    - 사용자가 직접 정의한 @ControllerAdvice 클래스를 사용하고 싶다면, 먼저 @Application 클래스에서 해당 @Bean 설정을 제거해야한다. 이렇게 하면, 사용자 정의 예외 처리 로직이 우선적으로 적용될 수 있다. 
- Cold Start 및 Spring Initialize 최적화
    - Snapstart를 실행해도, Cold Start에 따라 속도는 조금 저하된다. EventBridge에 Warmer용 람다를 설정해서, Snapstart alias가 적용된 람다버전을 Warming 해줬다.
    추가로, 람다 Warming은 Spring 인스턴스를 직접적으로 warming하지 않아서 Health-Check API를 통해 Spring 인스턴스도 Warming 해줬다. 
    여기서, 170ms는 api gateway에서 default로 소요되는 ms이다. target api가 같은 VPC에 있는 경우, 약 50ms 정도로 단축된다.
        > SnapStart민 적용시 1200ms
        > SnapStart + Cold Start Warmer invoke 적용시 400ms
        > SnapStart + Warmer invoke + call Health check 적용시 170ms 
- ComponentScan 
    - Spring 어노테이션은 패키지를 수신할 수 있으며 패키지의 모든 클래스에서 Spring 관련 구성, 리소스 및 Bean을 자동으로 스캔한다. 이것은 개발 중에 매우 유용하지만 클래스가 많아지면 그만큼 초기화 시간이 느려진다. 
        ~~~ java
        @Configuration
        @Import({ PetsController.class })
        public class PetStoreSpringAppConfig {
            ...
        }
        ~~~
        위의 클래스로 시작하면 Spring은 명령문에서 선언 한 클래스 (JVM에 의해이미로드되어 있음) 만 검사할 수 있으므로 전체 패키지를 스캔하는 무거운 작업을 피할 수 있다.
        개인적으론, Domain 영역이 많아지면 그만큼 무거워질수밖에 없다. 하지만, 인증서버는 도메인 영역 코드가 간단하고, 컴포넌트도 다양하지 않아서 무겁지는 않았다. 
- 생성자 주입 방지
    - 매개 변수 이름을 해당 Bean과 연관시키려면 Spring은 debug 플래그를 활성화 한 상태로 컴파일한다. Spring은 디스크에 관계를 캐시하므로 상당한 I/O 시간 패널티가 발생한다.
        ~~~ java
        public class Pet {
            @ConstructorProperties({"name", "breed"})
            public Pet(String name, String breed) {
                this.name = name;
                this.breed = synopsis;
            }
        }
        ~~~     
- SpringSecurity 
    - AWS Lambda의 실행 모델로 인해 서블릿 세션을 사용하여 값을 저장할 수 없다.
        ~~~ java
        @Order(1)
        @Configuration
        @EnableWebFluxSecurity
        public class SecurityConfig
        {
            @Bean
            public SecurityWebFilterChain securitygWebFilterChain(ServerHttpSecurity http) {
                return http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        }
        ~~~

## 전체 아키텍처
![인증서버_아키텍처](/assets/img/2024/240330/AuthLambda.png)

통합 인증서버를 적용하면서, 기존에 있었던 4가지 인증 방식은 다음과 같은 장점을 가졌다.

1. 사내 포탈 sso 연계의 경우, SSO 연계 이후 Cognito SAML을 통해 Cognito Token을 발급받아 초기 로그인 과정이 복잡하고 느린 단점이 극복되었다.
2. 팀내 포탈 연계의 경우, 별도의 인증 람다로 나눠져 있던 기능들이 하나의 람다로 통합되어 연계가 쉬워졌다. 
3. Authing(중국 인증)의 경우, 별도 region에서 연계되던 방식이라 관리의 이중화 문제가 있었는데 이부분이 해결되었다.
4. 부서내 backend 간 통신시 인증서버에서 ip별로 로그인 방식을 새로 추가할수 있어서 더 쉽게 통합이 되었다.
5. 기존의 인증 방식에 비해 최대 4배 정도 빠른 응답 속도를 얻게되었다. 
    > 200ms -> 50ms
6. JWT 방식의 인증 방식과 그외 인증 방식이 하나의 API로 통합되었다.

평균 소요시간은 다음과 같다.
- Token Generate: 400 ~ 600ms (3rd Party token 인증이 존재하여 조금 느림) / 기존 700 ~ 800ms
- Token Verify: 50ms / 기존 200 ~ 300ms 
- Token Logout: 100ms / 기존 200 ~ 300ms

## Lambda와 Concurrency

soft limit으로 aws root account에 각 region별 Full Account Concurrency가 존재.
이는 한 aws root account 에 해당 region의 모든 lambda 함수의 concurrent execution 값이 이 설정 값을 넘을 수 없다는 의미이다. 

예를 들면, 1개의 lambda 함수가 요청이 너무 많이 들어와서 concurrent execution이 현재 1000이라면 동 시간대에 다른 lambda 함수를 호출하면 모두 Throttle이 걸린다는 의미이다.

lambda가 항상 1000개의 concurrent execution이 즉시 가능한 상태로 대기하고 있지는 않는다. 요청이 들어오는 경우 lambda는 capacity를 증가시켜 주는데 최초에 burst하게 증가 시키는 concurrent execution을 Burst Concurrency라고 하고 여기에도 limit이 있다. seoul region의 경우, 500

최초 burst 이후에도 요청이 계속 대량으로 들어올 경우 lambda는 region에 관계 없이 concurrency를 분당 500 씩 증가시킨다. 

이를 해결하기 위해서, 2019년 Provisioned Concurrency 기능이 출시되었다. version이나 alias 별로, micro VM이 항시 초기화된 상태로 대기화해서 일관된 latency 응답을 보장한다. **단 latest version은 설정 불가**

![lambda_concurrency](https://docs.aws.amazon.com/images/lambda/latest/dg/images/concurrency-4-animation.gif)


## 동시성 계산법

일반적으로 시스템의 동시성은 둘 이상의 작업을 동시에 처리할 수 있는 기능이다. Lambda에서 동시성은 함수가 동시에 처리하는 진행 중인 요청의 수이다. 시간. Lambda 함수의 동시성을 측정하는 빠르고 실용적인 방법은 다음을 사용하는 것이다.

~~~
Concurrency = TPS * duration(by second)
~~~

예를 들어, 초당 100개의 request 받고 duration이 1초라면, concurrency는 100이다.
역으로, 평균 duration을 기준으로 몇개의 concurrency만 있다면, TPS를 추산할 수 있다. 

~~~
TPS = concurrency / duration(by second)
~~~

위의 verify API를 기준으로 예를 든다면, 15개의 concurrency만 있다면 300TPS가 해결된다. 

~~~
300TPS = 15(concurrency) / 0.05(second) 
~~~

그리하여, 여러 concurrency가 필요한 경우, warmer를 이용해서 특정 람다들이 동시에 띄울수 있게하면 provision concurrency와 같은 효과를 낼 수 있다. 
그리고, duration이 적을 수록 concurrency는 적은 수로도 많은 TPS를 처리할수 있다.

## 추가 고려사항
- reactor와 webflux를 활용한 비동기 인증체계를 염두해두고 있다.
    - 특히 DB 사용의 경우, r2dbc 고려 중
- spring boot 외에 spring cloud function만을 이용하여 reactive한 개발 환경도 고려하고있다. 
