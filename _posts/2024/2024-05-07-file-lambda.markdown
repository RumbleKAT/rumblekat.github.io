---
layout: post
title:  "S3 File Architecture, Lambda Invoke Proxy"
date:   2024-05-07 22:30:00 +0900
categories: dev
---

# 들어가면서

회사에서 파일관련 작업이나 Invoke 람다의 변경상황을 파악하는 일을 하는데 있어서 고민이 정말 많았다.
기존에 운영이 되고 있는 서비스에서, 신규 인프라 변경건에 대한 영향도를 분석할때나 시스템적인 제한사항들이 많았었는데,
이를 해결하고자 아래와 같은 방법들을 생각하고 적용하였다.

## File 처리 아키텍처

기존에 파일관련 작업은 아래와 같이 3가지 방식으로 운영되었다.

![파일01](/assets/img/2024/240507/20240507_01.png)

Case 1은 API Gateway 다음에 lambda나 ECS를 거치면서, S3를 접근하는 방식으로 S3 Util을 이용해서 손쉽게 비즈니스 코드를 작성할수 있는 장점이있다.
그렇지만, API Gateway의 timeout이 30초로 제한이 되어 있기 때문에, 대용량 처리에 적합하진 않았다. 더불에 API Gateway Request Size(10MB)가 존재하기 때문에 이 또한 제한상황이 존재했다. 

https://docs.aws.amazon.com/apigateway/latest/developerguide/limits.html

Case2는 API Gateway와 S3를 직접 연결하는 방식이다. 예를 들면 폴더와 리소스의 관계를 rest api처럼 표현하는 것으로 볼수 있다. 직관적인 사용을 할수 있지만, 이것 또한 payload 제한이 되는 방식이라 아쉬움이 존재했다. 

https://docs.aws.amazon.com/ko_kr/apigateway/latest/developerguide/integrating-api-with-aws-services-s3.html


Case 3는 Presigned URL을 활용하는 것이었다. Presigned URL은 특정 시간 제한을 두고 임시 접근 URL을 제공하는 방식이다. 이는 private bucket도 접근이 가능하다는 장점이 있었다. 또한, 용량도 훨씬 크고 Streaming을 이용한 더 빠른 파일 업,다운로드 처리도 가능해서 나는 이 방식을 채택했다.

https://docs.aws.amazon.com/ko_kr/AmazonS3/latest/userguide/PresignedUrlUploadObject.html


![파일02](/assets/img/2024/240507/20240507_02.png)

현재 사용 중인 파일 아키텍처는 다음과 같다. 적은 크기의 파일은 ECS에서 바로 처리해도 되나, NESTJS의 특성상 CPU Intensive한 작업을 지양하는 편이 좋기에 이러한 작업을 비동기 람다 호출을 통해서 해결하고자 했다.그리고 Redis에 현재 진척상황을 동기화하여 진행상황을 파악하였다.  

파일 시스템의 특성상 어떤 사용자가 어떤 파일을 요청했는지에 대한 기록이 필요할수 있어서, Presigned URL 호출 전에 DB에 파일 이력을 먼저 저장한 후 업로드 로직을 처리하도록 처리하였다.

업로드의 경우, 임시 폴더에 먼저 저장을 하고, 비동기 호출 람다에서 추가 작업이 필요하면 임시 폴더에서 먼저 처리하고 메인 폴더로 이동하는 방식으로 전략을 세웠다.

이를 통해 파일 관리와 성능과 편의성을 챙길수 있었다.


## Lambda Proxy Invoke

AWS Lambda를 사용하면서 람다끼리 호출되는 invoke를 많이 사용하는 경우, 이는 추후 유지보수나 영향도 파악에 있어서 큰 문제가 된다. 예를 들어 cloudformation stack 명이 바뀌기만 해도 이는 전체 시스템에 큰 영향을 준다.

우리는 내부에서 aws lambda invoke용 util을 개선하여, invoke를 할때 무조건 공통의 람다를 통하도록 만들었다. 

invoke를 호출하는 람다는 공통의 람다(Proxy Invoke Lambda)만을 호출하고, 다른 람다 호출 권한을 가지지 않는다. 더불어, 공통의 람다는 파라미터로 전달된 호출할 람다에 대한 권한을 가지고 람다를 실행한다.

이 과정에서 로깅으로 어떤 람다가 어디서 호출됬는지 중간 람다에서 로그 포맷을 남기고, 추후에 Athena에서 로그를 그룹핑하여 단번에 영향도 있는 작업을 살펴볼수 있다.

![파일02](/assets/img/2024/240507/20240507_03.png)
