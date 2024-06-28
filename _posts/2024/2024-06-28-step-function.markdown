---
layout: post
title:  "Step-Function, SnapStart"
date:   2024-06-28 21:06:00 +0900
categories: dev
---

# 들어가면서
인증서버를 4달간 유지하면서, 2번의 Sprint를 겪었었다. 2번의 Sprint 동안에는 로직에서 발샐하는 이슈는 없었고
인프라 부서에서 IAM을 수정하면서 특정 람다의 Cloudwatch write 권한이 빠졌다던지, Outbound Request를 제한함에 따라 API Gateway에 접근하지 못했었던
이슈로 인하여 2번정도 장애 상황이 발생했었다. 

이러한 장애상황을 겪으면서 Stack도 여러번 삭제를 진행했었는데, **SnapStart의 단점**이 여기서 들어난다.

## SnapStart의 단점
운영을 계속 해보면서, SnapStart의 문제점을 들수 있는 것이 있는데 그것은 바로 스택을 삭제하는데 시간이 오래걸린다는 것이다.
특히 버전이 오래 쌓일수록 삭제 되는 시간을 무시할수 없는 수준이다. 3개월간 운영하면서, 100개 이상의 SnapStart 버저닝이 생겼었는데 내부적으로 캐싱된 이미지를 계속 가지고 있어서
이를 모두 삭제하는데 오래걸렸다. 약 40분정도. 버전이 얼마 없으면 10분 내로 끝난다.

하지만, 캐시된 버전의 람다 이미지를 가지고 수행되는 기술이기 때문에, 속도적인 장점은 명확했으나 SnapStart가 수행될땐 몇개의 캐싱된 이미지의 람다를 동시에 몇개를 띄운다.
이렇게 되면 현재 람다가 몇개의 Invocation이 들어왔는지, Concurrency는 얼마나 소요되는지에 대한 정확한 계산을 유추하기 어려웠다. 
이 부분은 SnapStart를 걷어내면서 하나의 람다가 받는 부하에 대해서 깨달을 수 있었다. 

## SnapStart의 결론
물론, 이러한 단점에도 SnapStart는 절대로 나쁜 기술이라고는 말할 수 없다. 4개월동안 운영하면서 크게 장애가 발생한 적도 없었고 성능도 월등히 좋았기 때문이다.
aws lambda를 사용하면서 수정사항이 잘 없는 JVM 기반의 람다라면 충분히 이는 사용가치가 있다.

## SnapStart의 대안
SnapStart를 굳이 사용하지 않아도 비슷한 효과를 낼 순 있다. 그것은 바로 **serverless-warmer-plugin**과 Health Check API를 같이 사용하는 것이다.

![WarmerHealth](/assets/img/2024/240628/01.png)

warmer plugin은 event bridge에 등록되어 5분마다 특정 concurrency를 유지하게 해준다. 특히 이 플러그인은 특정 시간에 한하여 concurrency를 증폭시키고 비수기일땐 줄이고 할 수도 있다. 이 플러그인에서 invoke를 하면 JVM을 가동하는 lambda 인스턴스 자체를 워밍한다. 이렇게 되면 Cold Start가 먼저 예방된다. 그렇지만, JVM의 람다안에서 Spring 인스턴스를 띄운다면 Spring 인스턴스를 워밍하는 Health check 메서드를 호출해야한다. 

## 결론
인증 서버는 위의 기술을 활용하여 유동성있는 요청을 처리했다. 특히, 토큰을 검증하는 서버의 경우엔 평균 10ms 의 짧은 duration임에도 불구하고, 호출빈도 수가 많아서 많게는 20 concurrency가 가동될때가 있다. 그리하여, 모듈별로 검증하는 서버에 대한 람다를 분리하여 부하를 분산했다. 

# StepFunction
AWS Step Functions는 애플리케이션의 여러 AWS 서비스 간의 작업을 시각적으로 구성하고, 실행할 수 있게 해주는 서비스이다. 각 상태는 개발 작업을 나타낸다. 이를 통해 복잡한 비즈니스 로직을 단순화하고, 워크플로우를 관리할 수 있다.

## 주요개념
1. State Machine: 워크플로우를 정의한 JSON 파일로 구성된 AWS Step Function의 기본단위

2. State: 상태 머신 내의 각 단계이다. Task, Wait, Branch 등 다양한 유형이 있다.

- Task State(작업 상태)
    - 외부 서비스 또는 Lambda 함수와 같은 특정 작업을 수행한다.
    - 'Resource' 속성으로 수행할 작업을 지정한다.
    ``` json
        {
            "Type": "Task",
            "Resource": "arn:aws:lambda:us-east-1:123456789012:function:MyFunction",
            "Next": "NextState"
        }
    ```
- Choice State(조건 분기 상태)
    - 입력에 따라 여러 경로 중 하나를 선택
    - 'Choices' 속성으로 조건을 정의한다.
    ``` json
        {
            "Type": "Choice",
            "Choices": [
                {
                "Variable": "$.type",
                "StringEquals": "TypeA",
                "Next": "StateA"
                },
                {
                "Variable": "$.type",
                "StringEquals": "TypeB",
                "Next": "StateB"
                }
            ],
            "Default": "DefaultState"
        }
    ```
- Parralle State(병렬 상태)
    - 여러 분기를 병렬로 실행하고, 모든 분기가 완료될 때까지 기다린다.
    - 'Branches' 속성으로 병렬로 실행할 상태들을 정의한다.
    ``` json
    {
        "Type": "Parallel",
        "Branches": [
            {
            "StartAt": "ParallelState1",
            "States": {
                "ParallelState1": {
                "Type": "Task",
                "Resource": "arn:aws:lambda:us-east-1:123456789012:function:ParallelFunction1",
                "End": true
                }
            }
            },
            {
            "StartAt": "ParallelState2",
            "States": {
                "ParallelState2": {
                "Type": "Task",
                "Resource": "arn:aws:lambda:us-east-1:123456789012:function:ParallelFunction2",
                "End": true
                }
            }
            }
        ],
        "Next": "NextState"
    }
    ```
- Wait State(대기 상태)
    - 지정된 시간 동안 대기
    - Seconds 또는 Timestamp 속성으로 대기 시간을 정의한다.
    ``` json
    {
        "Type": "Wait",
        "Seconds": 10,
        "Next": "NextState"
    }
    ```
- Succeed State(성공 상태)
    - 상태 머신이 성공적으로 완료되었음을 나타낸다.
    - 추가 속성 없이 'Type' 만으로 정의한다.
    ``` json
    {
        "Type": "Succeed"
    }
    ```
- Fail State(실패 상태)
    - 상태 머신이 실패했음을 나타내고, 오류를 반환한다.
    - 'Error'와 'Cause'속성으로 오류 정보를 정의할 수 있다. 
    ``` json
    {
        "Type": "Fail",
        "Error": "ErrorCode",
        "Cause": "Error description"
    }
    ```
- Pass State(통과 상태)
    - 입력을 출력으로 전달하며, 데이터 변환 작업을 수행할수 있다.
    - 'Result' 속성으로 출력 데이터를 정의할 수 있다.
    ``` json
    {
        "Type": "Pass",
        "Result": {
            "key": "value"
        },
        "Next": "NextState"
    }
    ```

3. Task: Lambda 함수, ECS 작업 등 특정 작업을 수행하는 상태이다.

**상태 머신 예시**
~~~ json
{
  "StartAt": "InitialTask",
  "States": {
    "InitialTask": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:InitialFunction",
      "Next": "ChoiceState"
    },
    "ChoiceState": {
      "Type": "Choice",
      "Choices": [
        {
          "Variable": "$.status",
          "StringEquals": "success",
          "Next": "SuccessState"
        },
        {
          "Variable": "$.status",
          "StringEquals": "failure",
          "Next": "FailState"
        }
      ],
      "Default": "DefaultTask"
    },
    "SuccessState": {
      "Type": "Succeed"
    },
    "FailState": {
      "Type": "Fail",
      "Error": "TaskFailed",
      "Cause": "The task has failed"
    },
    "DefaultTask": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:DefaultFunction",
      "End": true
    }
  }
}
~~~

**State Machine의 순서**

1. InitialTask 상태에서 Lambda 함수를 호출
2. 호출 결과에 따라 ChoiceState 상태에서 경로를 선택
3. status가 "success"이면 SuccessState 상태로 이동하여 상태 머신을 성공적으로 종료
4. status가 "failure"이면 FailState 상태로 이동하여 오류를 반환
5. 그 외의 경우 DefaultTask 상태로 이동하여 또 다른 Lambda 함수를 호출하고 종료


## Map Reduce
그외에 Map State를 이용해서 Map Reduce 형태의 State Machine도 만들 수 있다. 추가로, MaxConcurrency를 사용해서 동시에 몇개의 람다 Task를 띄울것인지 설정도 가능하다.


![MapReduce](/assets/img/2024/240628/02.png)

### Map 작업을 수행하는 Lambda

문자열을 입력으로 받아 해당 문자열의 길이를 반환한다.

~~~ js
// mapFunction.js
exports.handler = async (event) => {
    const length = event.word.length;
    return {
        length: length
    };
};
~~~

### Reduce 작업을 수행하는 Lambda 
문자열 길이들의 목록을 입력으로 받아 합계를 계산한다.

~~~ js
// reduceFunction.js
exports.handler = async (event) => {
    const lengths = event.map(item => item.length);
    const totalLength = lengths.reduce((acc, length) => acc + length, 0);
    return {
        totalLength: totalLength
    };
};
~~~

Serverless.yml
~~~ yml
service: map-reduce-step-functions

provider:
  name: aws
  runtime: nodejs14.x
  region: us-east-1

functions:
  mapFunction:
    handler: mapFunction.handler

  reduceFunction:
    handler: reduceFunction.handler

stepFunctions:
  stateMachines:
    mapReduceStateMachine:
      definition:
        Comment: "A simple AWS Step Functions example for MapReduce using Map state"
        StartAt: MapTask
        States:
          MapTask:
            Type: Map
            MaxConcurrency: 2
            Iterator:
              StartAt: CalculateLength
              States:
                CalculateLength:
                  Type: Task
                  Resource: arn:aws:lambda:us-east-1:#{AWS::AccountId}:function:${self:service}-${opt:stage, self:provider.stage}-mapFunction
                  End: true
            ItemsPath: $.data
            ResultPath: $.mappedResults
            Next: ReduceTask
          ReduceTask:
            Type: Task
            Resource: arn:aws:lambda:us-east-1:#{AWS::AccountId}:function:${self:service}-${opt:stage, self:provider.stage}-reduceFunction
            End: true

plugins:
  - serverless-step-functions

~~~

**입력 예시**
~~~ json
{
  "data": [
    {"word": "apple"},
    {"word": "banana"},
    {"word": "cherry"},
    {"word": "date"},
    {"word": "elderberry"},
    {"word": "fig"},
    {"word": "grape"}
  ]
}
~~~

**결과값**
~~~ json
{
  "totalLength": 39
}
~~~
