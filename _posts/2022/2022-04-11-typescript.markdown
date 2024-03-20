---
layout: post
title:  "Typescript #1 기초 설정" 
date:   2022-04-11 21:00:00 +0900
categories: dev
---

# 들어가면서
Typescript를 오랬동안 써봤지만, 뭔가 기초가 탄탄한 느낌은 안들어서 이번에 정리를 다시 첨부터 해보고자 한다.
Typescript는 JS의 상위호환 언어이다. 그렇기 때문에 JS로 변환하는 빌드 과정을 잘 익혀야 한다.

# VS Code 상에서 빌드 설정
Mac 기준, 빌드를 수행하는 단축키는 Command + Shift + B이다. 이때 단축키를 눌렀을 때, 빌드 자동화를 수행하려면 테스크 러너를 설정해야한다. (.vscode/task.json) 이 파일을 제공하기 위해 vs code상에서 제공하는 명령어 
Command + Shift + P로 Configure Task 키워드를 선택한다. 

![샘플](/assets/img/0411/01.png)

타입스크립트에 적합한 테스크 템플릿이 없으므로, Others를 선택한다. 그러면 생성되는 파일은 다음과 같다.

~~~ json

{
    // See https://go.microsoft.com/fwlink/?LinkId=733558
    // for the documentation about the tasks.json format
    "version": "2.0.0",
    "tasks": [
        {
            "label": "echo",
            "type": "shell",
            "command": "echo Hello"
        }
    ]
}

~~~~

task.json에는 테스크라는 단위 작업을 추가할수 있다. 하지만, 디폴트 설정으론 타입스크립트를 돌릴 수 없으므로 타입스크립트 초기화 작업을 수행한다.
(즉, tsconfig.json 파일을 생성한다.)
~~~ 
$ tsc --init
~~~

Command + Shift + P를 누르고, Configure Task를 실행하면 2가지 옵션을 볼 수 있다.

- tsc watch : ts 파일이 변경될 때마다 컴파일
- tsc build : ts 파일을 바로 컴파일

그리고, 구성된 작업을 tsc watch로 설정해서 돌리면 아래와 같은 화면을 볼 수 있다.

~~~ json
{
    // See https://go.microsoft.com/fwlink/?LinkId=733558
    // for the documentation about the tasks.json format
    "version": "2.0.0",
    "tasks": [
        {
            "type": "typescript",
            "tsconfig": "tsconfig.json",
            "problemMatcher": [
                "$tsc"
            ],
            "group": "build",
            "label": "tsc: 빌드 - tsconfig.json"
        },
        {
            "type": "typescript",
            "tsconfig": "tsconfig.json",
            "option": "watch",
            "problemMatcher": [
                "$tsc-watch"
            ],
            "group": "build",
            "label": "tsc: 감시 - tsconfig.json"
        }
    ]
}

~~~
hello.ts
~~~ typescript
console.log("hello");
let a:number = 3;
console.log(a);
~~~

![샘플](/assets/img/0411/02.png)

hello.js
~~~ javascript
"use strict";
console.log("hello");
var a = 4;
console.log(a);
~~~

# 특정 파일만 빌드하도록 task.json 설정하기
Shell에서 typescript를 빌드하는 방법은 아래와 같다.
~~~ 
$ tsc hello.ts
~~~
이와 같은 tsc 명령어를 VS Code 상에서 수행하기 위해선, task.json을 수정한 후 빌드해준다.
~~~ json
{
    ...
    {
        "label" : "현재 TS 파일을 컴파일 하기",
        "type" : "shell",
        "command" : "tsc ${file} ",
        "group": {
            "kind": "build",
            "isDefault": true
        }
    }
}
~~~
- label : 사용자가 지정할 수 있는 테스크 명
- type : shell 값 입력시, 쉘 커맨드 실행
- command : 실제 수행되는 명령어 입력
    (감시모드 사용시, -w 붙여주면 된다. tsc -w ${file} )
- Group : 테스크가 속한 그룹을 설정

타입스크립트의 장점은 빌드하면서 타입 안정성을 해치는 문법들을 조기에 캐치하고 이를 가이드한다. 

![샘플](/assets/img/0411/03.png)

# TS-Node로 수행하기

~~~ json
{
    "label" : "현재 TS 파일을 TS-NODE로 실행하기",
    "type" : "shell",
    "command" : "ts-node ${file} ",
    "group": {
        "kind": "build",
        "isDefault": true
    }
}
~~~
![샘플](/assets/img/0411/04.png)

# TS-Node + TS-Lint 사용

TSlint 사용을 위해선 아래 명령어 수행후 tslint.json 파일에 rule을 넣는다.
~~~
$ tslint --init
~~~
tslint.json
~~~ json
{
    "defaultSeverity": "error",
    "extends": [],
    "jsRules": {},
    "rules": {
      "no-var-keyword" : true
    },
    "rulesDirectory": []
}
~~~

task.json
~~~ json
{
...
    {
        "label" : "현재 TS 파일을 TS-NODE로 실행과 TSlint 수행",
        "type" : "shell",
        "command" : "ts-node ${file} '&' tslint ${file}",
        "group": {
            "kind": "build",
            "isDefault": true
        },
        "presentation": {
            "reveal": "always",
            "panel": "new"
        }
    }
}
~~~
**lint 및 compile 수행시 (에러 케이스)**
![샘플](/assets/img/0411/05.png)

**lint 및 compile 수행시 (정상 케이스)**
![샘플](/assets/img/0411/06.png)

