---
layout: post
title:  "File Upload"
date:   2022-01-19 16:32:00 +0900
categories: dev
---

# 들어가면서
스프링 부트로 파일을 업로드 하는 것은 2가지 방식으로 구분할 수 있다.
1) 별도의 파일 업로드 라이브러리 사용(commons - fileupload 등)
2) Servlet 3버전부터 추가된 자체적인 파일 업로드 라이브러리를 이용

WAS의 버전이 낮거나 WAS가 아닌 환경에서 스프링부트 프로젝트를 실행한다면 별도의 라이브러리를 사용하는 것이 좋지만, 본 글에선 서블릿 기반으로 처리한다. 

# 파일 업로드를 위한 설정
스프링 부트에 내장된 Tomcat을 이용해서 실행한다면 추가적인 라이브러리 없이 application.properties 파일을 수정하면 가능하다.

~~~
spring.servlet.multipart.enabled=true
spring.servlet.multipart.location=./upload
spring.servlet.multipart.max-request-size=30MB
spring.servlet.multipart.max-file-size=10MB
com.rumblekat.upload.path="/upload"
~~~
위에서 부터 
1) 파일 업로드 가능 여부
2) 업로드된 파일의 임시저장 경로
3) 한 번에 최대 업로드 가능 용량
4) 파일 하나의 최대 크기


~~~ java

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@Log4j2
public class UploadController {

    @Value("${com.rumblekat.upload.path}")
    private String uploadPath;

    @PostMapping("/uploadAjax")
    public void uploadFile(MultipartFile [] uploadFiles){
        for(MultipartFile uploadFile : uploadFiles){

            //이미지 파일만 업로드 가능
            if(uploadFile.getContentType().startsWith("image") == false){
                log.warn("this file is not image type");
                return;
            }

            //실제 파일 이름 IE나 Edge는 전체 경로가 들어온다.
            String originalName = uploadFile.getOriginalFilename();
            String fileName = originalName.substring(originalName.lastIndexOf("\\")+1);
            log.info("filename : " + fileName);

            //날짜 폴더 생성
            String folderPath = makeFolder();

            //UUID
            String uuid = UUID.randomUUID().toString();

            //저장할 파일 이름 중간에 "_"를 이용해서 구분한다.
            String saveName = uploadPath + File.separator + folderPath +File.separator + uuid + "_" + fileName;

            Path savePath = Paths.get(saveName);

            try{
                uploadFile.transferTo(savePath);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private String makeFolder(){
        String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = str.replace("/", File.separator);

        //make folder
        File uploadPathFolder = new File(uploadPath, folderPath);

        if(uploadPathFolder.exists() == false){
            uploadPathFolder.mkdirs();
        }

        return folderPath;
    }
}

~~~

uploadFile()는 파라미터로 MultipartFile 배열을 받도록 설계한다. 배열을 활용하면, 여러개의 파일 정보를 처리할수 있고, 여러개의 파일을 동시에 업로드 할 수 있다. 브라우저에 따라 업로드하는 파일의 이름은 전체 경로일 수도 있고(IE 계열), 단순히 파일의 이름만을 의마할 수도 있음(chrome)

# 주의사항
파일을 저장하는 단계에선 아래 사항을 고려해야한다.
1) 업로드된 확장자가 이미지만 가능하도록 검사(첨부파일을 이용한 원격 쉘 공격 차단)
2) 동일한 이름의 파일이 업로드 된다면, 기존 파일을 덮어쓰는 문제
3) 업로드된 파일을 저장하는 폴더의 용량

## 1. 동일한 이름의 파일 문제
첨부파일의 이름이 같으면, 기존 파일이 삭제되고 새로운 파일로 변경될 수 있다. 이를 방지하기 위해서, 고유한 이름을 생성해서 파일이름을 사용한다. 가장 많이 사용하는 방식은 아래 2가지이다. 위의 방식은 UUID를 만들어서 처리한다. 

- 시간 값을 파일 이름에 추가
- UUID를 이용해서 고유한 값을 만들어서 사용

## 2. 동일한 폴더에 너무 많은 파일
업로드 되는 파일들을 동일한 폴더에 넣는다면 너무 많은 파일이 쌓이게 되고 성능도 저하된다. 특히 운영체제에 따라 최대로 넣을 수 있는 양이 다름(FAT32 방식은 65,534개 제한) 일반적으론 년/월/일 폴더를 따로 생성해서 한 폴더에 너무 많은 파일이 쌓이지 않게 한다.

## 3. 파일의 확장자 체크
첨부파일을 이용해서 '쉘 스크립트' 파일 등을 업로드해서 공격하는 기법도 있기 때문에 브라우저에서 파일을 업로드하는 순간이나 서버에서 파일을 저장하는 순간에도 이를 검사하는 과정을 거쳐야한다.
- MulipartFile에서 제공하는 getContentType()를 이용해서 처리할수 있다.

# 업로드 이미지 출력하기
JSON으로 반환된 업로드 결과를 화면에서 확인해주기 위해선 img 태그를 통해서 추가해줘야되고, 이미지 파일 데이터를 브라우저로 전달해야한다. 파일의 확장자에 따라서 브라우저에 전송하는 MIME 타입이 달라져야하는 문제는 java.nio.file 패키지의 Files.probeContentType()을 이용해서 처리하고, 파일 데이터의 처리는 스프링에서 제공하는 org.springframework.util.FileCopyUtils를 이용해서 처리한다.

~~~ java

  //upload 결과를 리턴한다.
    @GetMapping("/display")
    public ResponseEntity<byte[]> getFile(String fileName){
        ResponseEntity<byte[]> result = null;

        try{
            String srcFileName = URLDecoder.decode(fileName,"UTF-8");
            log.info("fileName : " + srcFileName );

            File file = new File(uploadPath + File.separator + srcFileName);
            log.info("file : " + file);
            HttpHeaders headers = new HttpHeaders();
            //MIME 타입 처리
            headers.add("Content-Type", Files.probeContentType(file.toPath()));
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file),headers, HttpStatus.OK);

        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

~~~

