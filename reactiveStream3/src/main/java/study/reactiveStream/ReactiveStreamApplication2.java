package study.reactiveStream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** Applying Web Application */
@SpringBootApplication
public class ReactiveStreamApplication2 {

    /*
    Spring MVC 3.2부터 Controller method를 Callable로 변경하여 비동기로 만들 수 있다.

    - Servlet 3.0: 비동기 서블릿
        - HTTP connection은 이미 논블록킹 IO
        - 서블릿 요청 읽기, 응답 쓰기는 블록킹
        - 비동기 작업이 시작되면 바로 서블릿 스레드를 반납하고, 작업이 완료되면 서블릿 스레드 재할당
        - 비동기 서블릿 컨텍스트 이용 (AsyncContext)
    - Servlet 3.1: 논블록킹 IO
        - 논블록킹 서블릿 요청, 응답 처리
        - Callback

    - 기본적으로 스레드의 블록킹은 CPU, 메모리 자원을 많이 소모하게 된다. (컨텍스트 스위칭 발생)
    스레드 블록킹 시 waiting 상태로 변경 + 컨텍스트 스위칭 -> i/o 작업 진행 -> running 상태 + 컨텍스트 스위칭
    이렇게 2번의 컨텍스트 스위칭이 일어나기 때문이다.

    - Java InputStream과 OutputStream은 블록킹 방식이며,
    RequestHttpServletRequest, RequestHttpServletResponse는 InputSream과 OutputStream을 사용하기 때문에
    서블릿은 기본적으로 "블로킹 IO 방식"이라고 할 수 있다.

    - 비동기 서블릿의 비동기 작업 수행 과정
    NIO Connector가 요청을 받으면 서블릿 스레드 풀에서 가져와서 서블릿 스레드를 계속 생성한다.
    만약 @Async 같은 걸로 다른 스레드 (작업 스레드)에게 작업을 넘기면, 해당 작업이 끝날 때까지 대기하는 것이 아니라
    그냥 바로 리턴이 가능하다.
    -> 그러나, http 요청의 경우 응답값이 떨어져야 하기 때문에 작업이 완료되었을 때
    빠르게 스레드를 할당하여 (매우 빠름!) 커넥터에 응답을 주고 바로 응답을 반납해준다.
 */


    @RestController
    public static class MyController {

        /** Async Servlet */
        // 리턴을 Callable로 감싸주게 되면 해당 처리를 스프링은 별도의 스레드를 만들어서 처리하도록 해준다/
        @GetMapping("/callable")
        public Callable<String> callable() throws InterruptedException {
            System.out.println(getCurrentThread() + "callable() Call!");
            return () -> {
                System.out.println(getCurrentThread() + "Working...");
                // 시간이 오래 걸리는 어떠한 작업이라고 가정
                Thread.sleep(2000);
                return "hello";
            };
           /*
            [http-nio-8080-exec-1] callable() Call!
            [task-1] Working...

            - 별도의 스레드에서 실행되고 리턴을 해준다.
            */
        }

        /** Sync Servlet */
        @GetMapping("/callable2")
        public String callable2() throws InterruptedException {
            System.out.println(getCurrentThread() + "callable2() Call!");
            Thread.sleep(2000);
            return "hello";
        }

        // DeferredResult를 저장해두는 queue 생성
        Queue<DeferredResult<String>> results = new ConcurrentLinkedDeque<>();

        /** Deferred Result Queue */
        @GetMapping("/dr")
        public DeferredResult<String> deferredResult() {
            System.out.println(getCurrentThread() + "deferredResult() Call!");
            // 파라미터로 타임아웃 설정
            DeferredResult<String> dr = new DeferredResult<>(600000L);
            // 저장해두기
            results.add(dr);
            return dr;
        }

        /** Return Queue Size */
        @GetMapping("/dr/count")
        public String drCount() {
            return String.valueOf(results.size());
        }

        /** Return Result Value */
        @GetMapping("/dr/event")
        public String drEvent(String message) {
            for(DeferredResult<String> dr : results) {
                dr.setResult("Hello " + message);
                results.remove(dr);
            }
            return "OK!";
        }

        /** Emitter */
        @GetMapping("/emitter")
        public ResponseBodyEmitter emitter() {
            // emitter는 비동기 요청 처리의 결과로 하나 이상의 응답을 위해 사용된다.
            // DeferredResult는 하나의 결과를 생성해서 요청을 처리했지만,
            // ResponseBodyEmitter는 여러 개의 결과를 만들어서 요청을 처리한다.
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();

            Executors.newSingleThreadExecutor().submit(() -> {
                // 시간이 걸리는 어떠한 작업이라고 가정
                try {
                    for (int i = 0; i < 50; i++) {
                        emitter.send("<p>Stream " + i + "</p>");
                        // 100ms만큼 작업을 진행하면서 클라이언트에게 결과 전달하기
                        Thread.sleep(100);
                    }
                } catch(IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            return emitter;
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(ReactiveStreamApplication2.class, args);
    }

}