---
layout: post
title:  "AWS Terraform 이론 정리"
date:   2023-05-02 21:15:00 +0900
categories: dev
---

# AWS
- 가용영역(AZ) 
    - 리전내에서 물리적으로 분리된 하나 이상의 데이터 센터

## VPC
격리된 가상의 네트워크 환경(CIDR 지정하여 생성)
- public subnet
    - 외부에서 접근이 가능, Internet Gateway를 통해 인터넷 접근
- private subnet
    - 외부에서 접근이 불가능, NAT Gateway를 경유해 인터넷 접근
- NAT Gateway
    - Private subnet 내의 리소스를 외부 인터넷과 연결시키는 게이트웨이(역방향은 불가)
- Public Route Table
    - public subnet에서 오는 트래픽 경로를 설정, Internet Gateway를 통해 외부로 나가도록 경로 설정
> Subnet Association - 서브넷과 Route Table을 연결
- Security Group
    - 인스턴스 자원에 대한 접근제어(white-list)

## AWS 예약된 주소
- 10.0.0.0 - 네트워크 자체 주소
- 10.0.0.1 - VPC 라우터 위한 주소
- 10.0.0.2 - DNS 서버 주소
- 10.0.0.3 - 향후 사용을 위해 예약한 주소
- 10.0.0.255 - 네트워크 브로드캐스트 주소


# Terraform
자원간의 종속성 자동 파악으로 수동으로 의존성을 정의할 필요없음
init - plan - apply 단계 수행
tf,tfvars 파일들을 한번에 다 모아서 실행, 알아서 의존성을 고려해서 수행한다.

- provider(AWS)
   - resource(create ec2, vpc, rds), module(소스 재사용)
   - output (출력)
   - variable (입력변수)
   - terraform(define version, backend)


## 파일 형식
- vars.tf
    - 필요한 변수를 정의하는 파일, 입력변수 참조의 경우, var.변수명 형식

- terraform.tfvars 
    - 정의된 변수에 값을 할당하는 파일(=vars보다 우선순위가 더높다.)
(=vars.tf에 정의된 값을 terraform.tfvars에서 다른 값으로 할당해 주는것이 가능)

- output
    - terraform 수행결과 출력 및 외부 참조목적 노출

- backend
    - terraform으로 관리하는 인프라 상태정보파일 저장위치(=tfstate)
    (= 설정할 원격지가 없으면 default로 워크스페이스 내 저장)

- module
    - 공통적으로 제활용할 리소스들을 하나로 모아서 정의한 객체

## Command
- init
    - 필요한 라이브러리 정보들을 가져옴
- plan
    - 자원의 결과값을 검증
- apply
    - 실제로 적용 (=tfstate 파일이 자동으로 생긴다.)
- destory
    - 일괄 삭제
- refresh
    - pull의 개념

> tfstate가 없으면, 새로 하나 생성한다. / terrform_lock은 여러명이 같이 관리할때 사용한다. 락에 대한 값을 dynamoDB에 저장한다.


# Git
- Untracked 
    - 최초 생성된 파일로 git에서 관리하지않는 상태
- Unmodified
    - clone해서 가지고 왔을때 상태
- Modified
    - 변경만하고 staging에 없는상태 *
- Staged
    - staging에 있는상태

## GitOps
- 소스코드 뿐만 아니라 배포, 설정 등 모든것을 코드화
- 단일진실 공급원 (=git)
- 선언형 코드를 통한 CD

## CD Type

pushType
- 전통적인 방식
- 저장소 내용 변경시 Deployment Pipeline 실행
- Jenkins, CircleCI

pullType
- 배포환경의 Agent가 Deployment Pipeline 대신함
- 저장소와 배포환경 지속적으로 비교 및 Sync
- ArgoCD, FluxCD
(보안을 위해 Pull Type 권고)

PushType의 경우, 
- CI/CD 서버에서 kubectl tool을 설치하고 설정해야됨
- CI/CD 서버 내부에서 k8s 접속 위한 설정 및 key 저장
- 배포 이후 상태 모니터링 불가

PullType의 경우,
- 지속적 모니터링 통해 인프라의 상태 업데이트
- Git이 아닌 외부에서 클러스터링 직접 변경 불가능
- CI/CD 서버 내부에 Kubernetes 접속 설정 불필요
- k8s만 지원하는 경우가 대부분

## GitOps의 장점
- 장애복구시간 감소
- 신뢰할수있는 정보 공유
- 인적실수 방지하여 안정성 향상되고 높은 신뢰성 제공

# Jenkins & ArgoCD
## jenkins 
- 빌드, 테스트, 배포 등 모든것을 자동화 해주는 서버
- 다양한 plugin을 활용하여 자동화 작업처리
- pipeline을 통해 CI/CD 파이프라인 구축

Credentials
Global Scope - Jenkins내 제약없이 사용
 - github token
 - AWS 서비스 생성을 위해 필요한 IAM Access Key 정보
System Scope - email인증이나 agent 연결등 jenkins 자체 시스템관리 기능만을 위해 사용하는 credential 생성 및 활용

Kubernetes plugin 
- Jenkins master(명령) - slave(빌드 잡 수행) 
-> 쿠버네티스에 jenkins를 구축하는 경우, API call을 통해 slave pod 생성
   slave pod에서 pipeline 수행 -> Job 수행완료시 Slave pod 삭제

Pipeline
- 연속적인 이벤트 또는 job을 실행하기 위한 plug-in

agent section
- pipeline을 어떤 node, agent가 실행할지 정의
- terraform 컨테이너가 있는 pod에서 pipeline을 실행하도록 정의

stage section
- 어떤 일을 처리해야할지 stage를 정의함
step 
- stage 안에서 실행될 하나 이상의 단계

Blue Ocean 
- pipeline의 보다쉬운 사용을 위한 UI제공
- 문제 발생시 정확하게 확인가능

## ArgoCD
application manifest file이 저장된 git repository 주기적으로 모니터링
git에 정의된 application manifest 형상과 k8s에 배포된 application의 형상 비교

- API server (webUI, CLI 및 다른 CI/CD 시스템에서 API 요청을 받고 처리하는 서버)
- Application Controller (Application 상태를 계속 모니터링하면서, k8s에 배포된 형상과 repository manifest에 정의된 형상을 비교해서 sync 작업을 수행한다.)

git/ Helm Chat Repo 추가 가능
잘만들어진 Helm 가져와서 Git으로 관리하는것을 권장