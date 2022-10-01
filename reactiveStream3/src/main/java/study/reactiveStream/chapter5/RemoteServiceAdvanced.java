package study.reactiveStream.chapter5;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 조금 더 다양한 일을 수행하는 RemoteServiceAdvanced 구성 */
@SpringBootApplication
public class RemoteServiceAdvanced {

    @RestController
    public static class MyController {
        @GetMapping("/service1")
        public String service1(String req) throws InterruptedException {
            // 2초 정도 걸리는 어떠한 작업이라고 가정
            Thread.sleep(2000);
            return req + "/service1";
        }

        @GetMapping("/service2")
        public String service2(String req) throws InterruptedException {
            // 2초 정도 걸리는 어떠한 작업이라고 가정
            Thread.sleep(2000);
            return req + "/service2";
        }
    }

    public static void main(String[] args) {
        // 한 프로젝트에서 띄우는 거니까 포트번호가 겹치지 않도록 하기 위해서 8082번으로 띄우기
        System.setProperty("server.port", "8082");
        // 스레드 속성도 겹치지 않도록 하기 위해서 별도로 설정
        System.setProperty("server.tomcat.threads.max", "1000");
        SpringApplication.run(RemoteServiceAdvanced.class, args);
    }
}
