---
layout: post
title:  "Middy Router"
date:   2024-05-06 17:35:00 +0900
categories: dev
---

# 들어가면서

Middy는 현재 버전을 기준으로 Http Router, WebSocket Router 등이 존재한다. 이는 Node.js 기반의 AWS Lambda 함수에서의 
요청을 간소화하고 개선하기 위해 등장했다. 하지만, CloudFormation 한계상 Deploy하는 Resource의 갯수의 한계(500개)가 있고,
이에 따라 Serverless Framework의 Split Stack과 같은 방식으로 많은 리소스를 한번에 배포하는 방식을 차용하는 경우가 많았다.

문제는 Split Stack을 사용하면 그만큼 배포 시간이 더 소요된다는 문제가 있어서 이는 DevOps 담당자들에게 큰 고민거리였다.
Middy 4.0버전 이후부턴 Http Router를 제공하여 단일 람다에 여러가지 API를 통합시킬수 있는 기능을 제공한다. 
이전에는 이를 해결하려면 API Gateway상에서 와일드카드 {Proxy+} 를 사용해서, 동적으로 Routing을 시켜주는 람다를 만들었었는데
현재는 Middy에서 이 기능을 제공해주고 있어서 더 편하게 많은 양의 배포 리소스를 줄일수 있었다.

## Middy의 등장 배경

1. 서버리스 아키텍쳐의 등장 
  - 클라우드 컴퓨팅의 발전과 함께, AWS Lambda와 같은 서비스는 서버의 관리 없이 코드 실행이 가능하게 하여 개발자들이 인프라 관리보다
    비즈니스 로직에 더 집중하게 해줌

2. 기능의 제한성
  - AWS Lambda는 매우 유용하지만, 복잡한 라우팅, 요청 파싱, 에러 해들링 등을 처리하려면 추가적인 로직이 필요

3. 중복 코드 문제
  - 서버리스 함수 간에 공통적으로 필요한 기능들을 각 함수에 반복해서 작성해야 하는 경우가 많은데, 이는 유지보수성을 저하시키고 
    에러 가능성을 높임

## Middy Http Router의 등장

1. 라우팅 간소화
 - 복잡한 API Endpoint 관리를 위해, Http 요청을 각각의 처리 로직으로 라우팅하는 기능을 제공 (Ex, Serverless-Express와 비슷)

2. 코드 재사용 및 간소화
 - 공통된 요청 처리 로직을 미들웨어로 분리하고, 코드의 중복을 줄여서 간결한 로직을 만들수 있다.

3. 서버리스 환경 최적화
 - Lambda와 같은 서버리스 환경에서 Http 요청을 효과적으로 처리

## 예시 코드

하기 소스의 예시를 살펴보면, handler 안에서 각 요청에 따른 실행 handler를 등록하고 이를 실행시킨다.
사실 Vendia의 Serverless-Express도 이와 비슷한 원리로 돌아간다. 그렇지만, Middy와 Serverless-Express의 결정적인 차이는 AWS 좀더 친화적인것이 Middy 쪽이라는 것이다.

~~~ js

import middy from '@middy/core'
import httpRouterHandler from '@middy/http-router'
import validatorMiddleware from '@middy/validator'

const getHandler = middy()
  .use(validatorMiddleware({eventSchema: {...} }))
  .handler((event, context) => {
    return {
      statusCode: 200,
      body: '{...}'
    }
  })

const postHandler = middy()
  .use(validatorMiddleware({eventSchema: {...} }))
  .handler((event, context) => {
    return {
      statusCode: 200,
      body: '{...}'
    }
  })

const routes = [
  {
    method: 'GET',
    path: '/user/{id}',
    handler: getHandler
  },
  {
    method: 'POST',
    path: '/user',
    handler: postHandler
  }
]

export const apiHandler = middy()
  .use(httpHeaderNormalizer())
  .handler(httpRouterHandler(routes))

~~~

라우팅을 하는 방식은 아래 예시를 참고한다. 

~~~ yml

functions:
  apiHandler:
    handler: handler.apiHandler
    events:
      - http:
          path: /
          method: ANY
      - http:
          path: /{proxy+}
          method: ANY
~~~

아니면 아래 예시처럼 라우팅 할 path를 직접 지정하는 방법도 있다. 이 경우, http path가 바뀌면 Cloudformation이 업데이트 될때 Crashed 되는 문제가 생길수 있으므로 조심해야한다. 
(보통 API Gateway를 Console에서 직접 지정한 경우와 IaC 방식으로 업데이트를 치는 방식이 충돌하는 경우에 위와 같은 Crashed 현상이 많이 발생한다. 이때는 둘중의 하나를 통일시켜주는 방식으로 재배포하면 해결된다.)

~~~ yml

functions:
  apiHandler:
    handler: handler.apiHandler
    events:
      - http:
          path: /user/{id}
          method: GET
      - http:
          path: /user
          method: GET
      - http:
          path: /user
          method: POST
~~~         


## Serverless-Express와 차이

Serverless-Express는 AWS Lambda 위에서 Express.js를 실행시키기 위한 프로젝트이다. Java의 SpringBoot나 Spring Cloud Function처럼 AWS Lambda 위에서 올리는 것과 비슷하다.
그렇지만, 결정적인 차이점은 Serverless-Express는 Lambda Invoke를 허용하지 않는다. 그렇기 때문에 Cold Start의 문제점이 있다고 볼 수 있는데, Middy Http Router는 이를
극복할수 있다. 
