---
layout: post
title:  "Typescript #2 기초 설정" 
date:   2022-04-14 20:00:00 +0900
categories: dev
---

# 들어가면서
tsconfig는 다양한 설정들을 넣을 수 있는 옵션들이 있다. 특히 tsc 명령어를 커맨드 라인에서 칠 때, 해당 프로젝트의 tsconfig.json을 기준으로 컴파일을 수행한다. 

만약 현재 디렉토리에 tsconfig가 없다면 상위 디렉토리러 이동하면서 찾는다. 여기서 중요한 점은 tsconfig.json 파일이 위치한 경로가 타입스크립트 프로젝트의 루트 경로가 된다는 것이다.*(만약 tsconfig.json 파일을 찾지 못하면, "tsc --help"를 실행했을 때 와 같은 결과를 출력해서 컴파일 옵션을 추가해 명령어를 실행하라고 안내한다.)*

## tsconfig.json의 특징
- target : 컴파일 후 변환될 자바스크립트 버전
- module : 모듈형식을 지정한다.
- sourceMap : 컴파일 후 맵 파일의 생성 여부를 결정한다.
    (https://minhyeong-jang.github.io/2019/05/24/sourcemap)
    => **코드상의 위치를 기억하고 알려주기 때문에 빌드 전 어떤 파일, 라인에서 오류가 났는지 확인할 수 있다.**
- strict : true이면 엄격 타입검사 모드를 활성화시킨다. 
- noImplictAny : any 타입으로 암묵적 형변환 여부를 결정한다. 해당 속성이 생략되면 기본 값은 false 
=> **타입스크립트에서 타입을 선언하지 않아도 타입 에러가 발생하지 않는다.**
- esModuleInterop : 자바스크립트 모듈과 상호 운용성을 가능하게 하는 속성으로 true면 CommonJS모듈을 디폴트 모듈처럼 호출할 수 있다.


## 컴파일 결과를 특정 디렉토리에 생성하기

~~~ json

{
  "compilerOptions": {
    "target": "es5",                          /* Specify ECMAScript target version: 'ES3' (default), 'ES5', 'ES2015', 'ES2016', 'ES2017', 'ES2018', 'ES2019', 'ES2020', or 'ESNEXT'. */
    "module": "commonjs",                     /* Specify module code generation: 'none', 'commonjs', 'amd', 'system', 'umd', 'es2015', 'es2020', or 'ESNext'. */
    "outDir": "./dist-outdir",                        /* Redirect output structure to the directory. */
    "strict": true,                           /* Enable all strict type-checking options. */
    "esModuleInterop": true,                  /* Enables emit interoperability between CommonJS and ES Modules via creation of namespace objects for all imports. Implies 'allowSyntheticDefaultImports'. */
    "forceConsistentCasingInFileNames": true  /* Disallow inconsistently-cased references to the same file. */
  },
  "include" : [
    "src/**/*"
  ],
  "exclude": [
    "node_modules",
    ".vscode"
  ]
}

~~~

![샘플](/assets/img/0414/01.png)

## tsconfig.json 속성 확장하기
만약 tsconfig.json에 공통 설정 파일을 부모설정 파일로 두고 이를 자석 설정 파일에서 확장하려면 extends 속성을 이용한다. 

config/base.json
~~~ json
{
    "compilerOptions" :{
        "removeComments" : true,
        "sourceMap" : true
    },
    "exclude" : [
        "node_modules",
        ".vscode"
    ]
}
~~~

tsconfig-extents.json
~~~ json
{
    "extends" : "./config/base.json",
    "compilerOptions": {
        "outFile": "./dist/extends/out.js",
        "target": "es5"
    }
}
~~~
Shell
~~~ 
❯ tsc --build tsconfig-extends.json
~~~

Hello.ts
~~~ typescript
console.log("hello");
let a:number = 11;
let b:string = "sss"
console.log(b);
~~~

Hello2.ts
~~~ typescript
let hello2:string = "111"
console.log(hello2);

function add(paramA : string, paramB : number): void{
    console.log(`${paramA} + ${paramB}`);
}
~~~
변환된 js => out.js
~~~ javascript

console.log("hello");
var a = 11;
var b = "sss";
console.log(b);
var hello2 = "111";
console.log(hello2);
function add(paramA, paramB) {
    console.log(paramA + " + " + paramB);
}
//# sourceMappingURL=out.js.map
~~~