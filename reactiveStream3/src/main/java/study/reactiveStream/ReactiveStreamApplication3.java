package study.reactiveStream;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@SpringBootApplication
@EnableAsync
public class ReactiveStreamApplication3 {

    @RestController
    @SuppressWarnings("deprecation")
    public static class MyController {
        public static final String HTTP_LOCALHOST_8082_SERVICE_1_REQ = "http://localhost:8082/service1?req={req}";
        public static final String HTTP_LOCALHOST_8082_SERVICE_2_REQ = "http://localhost:8082/service2?req={req}";

        // 외부의 서비스에 api 호출을 진행하도록 하기 위해서. (우리가 만든 remoteService 활용)
        /** RestTemplate - Blocking 방식 + Multi-Thread */
        RestTemplate rt = new RestTemplate();

        /** AsyncRestTemplate - Spring에서 제공하는 Non-Blocking 방식. + Multi-Thread */
        // 근데 5.0부터 Deprecated 되었기 때문에 웬만하면 WebClient를 사용하자...!
        AsyncRestTemplate art = new AsyncRestTemplate();

        /** AsyncRestTemplate - With Netty : Single Thread */
        // 사실 이 친구도 deprecated 되었다... ReactorClientHttpConnector를 사용하는 게 맞을 듯.
        AsyncRestTemplate artn = new AsyncRestTemplate(
                // netty의 스레드도 1개로 제한하기
                // 기본적으로 netty는 프로세스 개수 * 2개가 디폴트로 생성된다!
                new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));


        @GetMapping("/hello")
        public ListenableFuture<ResponseEntity<String>> hello(int idx) {

            /** RestTemplate 사용*/
            // restTemplate의 getForObject는 blocking method이다. (요청이 다시 들어올 때까지 기다림)
            // 그렇기 때문에 시간이 오래 걸린다. 이를 위해서 비동기적으로 바꾸어보자.
//            return rt.getForObject("http://localhost:8081/service?req={req}", String.class,
//                    ("hello " + idx));

            /** AsyncRestTemplate 사용*/
            // getForObject의 경우 단순히 responseBody 값을 리턴했었는데, getForEntity의 경우 헤더 + 응답코드 + 바디값까지 함께 리턴한다.
            // 반환형이 Listenable<ResponseEntity<String>>형인데, 컨트롤러에서 반환해주면 스프링이 알아서 콜백함수 등록해서 성공적으로 리턴을 해준다.
//            return art.getForEntity("http://localhost:8081/service?req={req}", String.class, ("hello " + idx));

            /** AsyncRestTemplate with Netty */
            return artn.getForEntity("http://localhost:8081/service?req={req}", String.class, ("hello " + idx));
        }

        @GetMapping("/hello-advanced")
        public DeferredResult<String> hello2(int idx) {
            // 스프링에게 결과값을 넘겨주기 위해서 DeferredResult 활용하기
            // 얘를 사용하지 않으면 어차피 콜백에서만 실행되고 끝나기 때문에 스프링에게 전달하기 위해 따로 담아줄 곳이 필요한 것.
            DeferredResult<String> dr = new DeferredResult<>();

            /** AsyncRestTemplate with Netty - 결과값 가공하기 */
            ListenableFuture<ResponseEntity<String>> lf1 = artn.getForEntity("http://localhost:8081/service?req={req}", String.class, ("hello " + idx));
            lf1.addCallback(s -> {
                // success 시 s에 ResponseEntity<String> 값이 들어가있으니까 body값을 꺼내보자.
                dr.setResult(s.getBody() + "/work"); // 추가적으로 /work 붙여서 확인하기
            }, e -> {
                // 예외 발생 시 deferredResult의 errorResult 활용하기
                dr.setErrorResult(e.getMessage());
            });

            return dr;
        }

        /** RemoteServer - Advanced */
        @GetMapping("/hello-multi")
        public DeferredResult<String> hello3(int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> lf1 = artn.getForEntity("http://localhost:8082/service1?req={req}", String.class, ("hello " + idx));
            lf1.addCallback(s1 -> {
                // null 처리
                assert s1 != null;

                // 조금 더 복잡한 예제를 보기 위해서 내부에서 또 다른 요청 진행
                ListenableFuture<ResponseEntity<String>> lf2 = artn.getForEntity("http://localhost:8082/service2?req={req}", String.class, s1.getBody());
                // 콜백 등록
                lf2.addCallback(s2 -> {
                    assert s2 != null;
                    dr.setResult(s2.getBody());
                }, e2 -> {
                    dr.setErrorResult(e2.getMessage());
                });

            }, e1 -> {
                dr.setErrorResult(e1.getMessage());
            });

            return dr;
        }

        @Autowired
        MyService myService;

        /** Add Sevice Layer  */
        @GetMapping("/hello-service")
        public DeferredResult<String> hello4(int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            ListenableFuture<ResponseEntity<String>> lf1 = artn.getForEntity(HTTP_LOCALHOST_8082_SERVICE_1_REQ, String.class, ("hello " + idx));
            lf1.addCallback(s1 -> {
                // null 처리
                assert s1 != null;

                // 조금 더 복잡한 예제를 보기 위해서 내부에서 또 다른 요청 진행
                ListenableFuture<ResponseEntity<String>> lf2 = artn.getForEntity(HTTP_LOCALHOST_8082_SERVICE_2_REQ, String.class, s1.getBody());
                // 콜백 등록
                lf2.addCallback(s2 -> {
                    assert s2 != null;
                    // 서비스의 리턴형도 ListenableFuture니까 또 다시 callback 걸어주기
                    ListenableFuture<String> lf3 = myService.work(s2.getBody());
                    lf3.addCallback(s3 -> {
                        dr.setResult(s3);
                    }, e3 -> {
                        dr.setErrorResult(e3.getMessage());
                    });
                }, e2 -> {
                    dr.setErrorResult(e2.getMessage());
                });

            }, e1 -> {
                dr.setErrorResult(e1.getMessage());
            });

            return dr;
        }
    }

    /** Service단 추가 */
    @Service
    public static class MyService {
        // 비동기 처리를 위해 Asnyc annotation 추가
        @Async(value = "myThreadPool")
        // 스레드 정책이 SimpleAsyncTaskExecutor이라 매번 새롭게 생성하니까 커스텀 스레드 정책 설정해주기
        public ListenableFuture<String> work (String req) {
            return new AsyncResult<>(req + "/asyncwork");
        }
    }

    /** 스레드 개수 제한해주기 */
    @Bean
    public ThreadPoolTaskExecutor myThreadPool() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        // 1개로 제한해두기
        te.setCorePoolSize(1);
        te.setMaxPoolSize(10);
        te.initialize();
        return te;
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveStreamApplication3.class, args);
    }
}
