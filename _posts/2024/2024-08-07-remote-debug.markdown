---
layout: post
title:  "JAVA Remote Debug"
date:   2024-08-07 21:58:00 +0900
categories: dev
---

## JDB(Java Debugger)

**JDB (Java Debugger)** 는 Java 프로그램을 디버그하는 명령줄 기반 도구입니다. Java Development Kit (JDK)에 포함되어 있으며 Java Debug Interface (JDI)를 통해 작동합니다.

#### 주요 기능과 명령
1. **프로그램 실행과 연결**
   - `jdb <class>`: 클래스를 로드하여 디버깅 시작.
   - `jdb -attach <host>:<port>`: 원격 JVM에 연결하여 디버깅.

2. **브레이크포인트 설정**
   - `stop at <class>:<line>`: 특정 줄에 브레이크포인트 설정.
   - `stop in <class>.<method>`: 특정 메소드에 브레이크포인트 설정.

3. **실행 제어**
   - `run`: 프로그램 실행 시작.
   - `cont`: 프로그램 실행 계속.
   - `step`: 다음 줄로 이동.
   - `next`: 현재 줄을 실행하고 다음 줄로 이동.

4. **정보 검사**
   - `print <expression>`: 표현식 값 출력.
   - `locals`: 현재 로컬 변수 출력.
   - `dump <object>`: 객체의 모든 필드 값 출력.

5. **스택 트레이스**
   - `where`: 현재 스레드의 스택 트레이스 출력.

6. **스레드 제어**
   - `threads`: 실행 중인 모든 스레드 나열.
   - `thread <thread_id>`: 특정 스레드로 전환.

#### 사용 예시
```sh
jdb MyClass
stop in MyClass.main
run
print myVariable
step
next
cont
```

### JDB에서 사용되는 프로토콜

**Java Debug Wire Protocol (JDWP)**는 JDB에서 사용되는 주요 프로토콜입니다. JDWP는 디버거와 JVM 사이의 통신을 담당하여 브레이크포인트, 스텝 실행, 변수 검사 등의 디버깅 작업을 지원합니다.

### 비슷한 기술

1. **GDB (GNU Debugger)**
   - 주로 C/C++ 프로그램을 디버깅하는 데 사용됩니다.
   - 다양한 브레이크포인트 설정, 스택 트레이스 검사, 변수 값 변경 등을 지원합니다.
   - GDB 서버와 클라이언트 모델을 통해 원격 디버깅도 가능.

2. **LLDB**
   - LLVM 프로젝트의 디버거로, C, C++, Objective-C 및 Swift 프로그램을 디버깅합니다.
   - GDB와 유사한 기능을 제공하며, Xcode와 통합되어 사용됩니다.

3. **WinDbg**
   - Windows 운영 체제에서 주로 사용되는 디버거입니다.
   - 커널 모드 및 사용자 모드 디버깅을 지원하며, Windows 드라이버 및 애플리케이션 디버깅에 주로 사용됩니다.

4. **Eclipse Debugger**
   - Eclipse IDE에서 제공하는 디버거로, Java를 포함한 여러 언어를 디버깅할 수 있습니다.
   - 그래픽 사용자 인터페이스를 제공하여 사용하기 쉽습니다.

5. **Visual Studio Debugger**
   - Visual Studio IDE에서 제공하는 디버거로, 주로 C#, C++, VB.NET 등의 언어를 디버깅합니다.
   - 강력한 기능과 직관적인 사용자 인터페이스를 제공합니다.

JDB는 JDWP를 사용하여 Java 프로그램을 디버깅하는 명령줄 도구입니다. 유사한 디버깅 도구로는 GDB, LLDB, WinDbg, Eclipse Debugger, Visual Studio Debugger 등이 있습니다. 이들 도구는 각각의 환경과 언어에 맞춰 다양한 디버깅 기능을 제공합니다.

## VSCODE에서 원격서버 디버깅 예시

먼저, 아래와 같은 소스를 ubuntu 기반의 서버에서 호스팅한다고 가정해보겠습니다. 
(실제는 wsl2 기반으로 테스트를 진행했습니다.)

``` java

import java.util.concurrent.CountDownLatch;

public class OurApplication {
    private static String staticString = "Static String";
    private String instanceString;
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) {
        for (int i = 0; i < 1_000_000_000; i++) {
            OurApplication app = new OurApplication(i);
            System.out.println(app.instanceString);
        }

        System.out.println("Press Ctrl+C to exit...");
        try {
            latch.await(); // CountDownLatch를 이용하여 무한 대기
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public OurApplication(int index) {
        this.instanceString = buildInstanceString(index);
    }

    public String buildInstanceString(int number) {
        return number + ". Instance String !";
    }
}

```

그리고 DockerFile을 만들땐 jdwp를 사용하도록 인자를 추가해주세요.

``` java
# Base image 선택
FROM openjdk:11-jdk

# 애플리케이션 복사
COPY . /app
WORKDIR /app

# 애플리케이션 컴파일
RUN javac OurApplication.java

# 디버깅 포트 노출
EXPOSE 5005

# ENTRYPOINT와 CMD 조합으로 인자 전달
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "OurApplication"]
```

그리고 docker image를 빌드하고, docker에 호스팅합니다.

```
# docker 빌드
docker build -t java-debug .

# docker 컨테이너 실행
docker run -d -p 5005:5005 java-debug

```

컨테이너를 실행하고, window 환경에서 OurApplication.java 소스가 있다는 전제하에 .vscode/launch.json 에 원격디버깅 설정을 넣어주세요.
(local 서버환경이랑 dev 서버환경이 분리된 것을 기준으로 작성하고 있습니다. window 환경은 여기서 local 환경입니다.)

``` json
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "OurApplication",
            "request": "launch",
            "mainClass": "OurApplication",
            "projectName": "remote_89c5668d"
        },
        {
            "type": "java",
            "name": "Attach to Remote Program",
            "request": "attach",
            "hostName": "localhost", 
            "port": 5005
        }
    ]
}

```

그리고 디버그 모드로 실행을 하면, launch.json 기준으로 설정으로 인하여, ubuntu 서버와 디버깅 커넥션이 맺어지게 됩니다.

![debug1](/assets/img/2024/240807/01.png)


그리고 VARIABLES에서는 현재 스택에서 사용되는 변수 정보를 볼수 있고, WATCH에는 사용자가 확인하고 싶은 변수의 정보를 확인할수 있습니다. 
단, 여기서는 변수는 수정은 불가합니다.

![debug2](/assets/img/2024/240807/02.png)


하단의 DEBUG CONSOLE에선 변수의 수정이 가능합니다. 그리고, 여기서 수기로 사용자가 원하는 값을 설정하는 것이 가능합니다.

![debug3](/assets/img/2024/240807/03.png)

변경된 값에 대한 출력은 TERMINAL 탭에서 확인할 수 있습니다.
![debug4](/assets/img/2024/240807/04.png)

![debug5](/assets/img/2024/240807/05.png)

위의 방식으로, 원격으로 운영환경에서 디버깅하는 것이 가능합니다. 