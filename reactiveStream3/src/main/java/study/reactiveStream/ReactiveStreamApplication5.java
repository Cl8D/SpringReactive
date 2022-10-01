package study.reactiveStream;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

/** CompletableFuture 적용하기 */
@SpringBootApplication
@EnableAsync
public class ReactiveStreamApplication5 {

    @RestController
    @SuppressWarnings("deprecation")
    public static class MyController {
        public static final String URL1 = "http://localhost:8082/service1?req={req}";
        public static final String URL2 = "http://localhost:8082/service2?req={req}";

        AsyncRestTemplate artn = new AsyncRestTemplate(
                new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @Autowired
        MyService myService;

        @GetMapping("/hello-service-cf")
        public DeferredResult<String> hello(int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            // CompletableFuture로 바꾸었기 때문에 chaining이 가능하다.
            toCF(artn.getForEntity(URL1, String.class, ("hello " + idx)))
                    // toCF로 반환되는 값이 또 다시 CompletableFuture이니까 thenCompose
                    .thenCompose(s1 -> toCF(artn.getForEntity(URL2, String.class, s1.getBody())))
                    // service단 코드를 동기적으로 변환하고, 여기서 별도의 스레드로 처리할 수 있도록 코드 변경 (조금 더 간결해짐)
                    .thenApplyAsync(s2 -> myService.work(s2.getBody()))
                    .thenAccept(dr::setResult)
                    // 정상적인 상황이었으면 위에서 종료되었겠지만 예외가 발생했으면 위에서 타고 내려왔을 거니까 아래에서 잡기
                    .exceptionally(e -> {
                        // exceptionally는 리턴값이 필요해서 그냥 의미없는 값 리턴해주기
                        dr.setErrorResult(e.getMessage());
                        return null;
                    });
            return dr;
        }

        /** CompletableFuture 형태로 전환하기 */
        <T> CompletableFuture<T> toCF (ListenableFuture<T> lf) {
            // 완료되지 않은 비동기 작업을 선언해둠.
            CompletableFuture<T> cf = new CompletableFuture<T>();
            // 작업이 완료되었다고 알려주기
            lf.addCallback(cf::complete, cf::completeExceptionally);
            return cf;
        }
    }

    /** Service단 추가 */
    @Service
    public static class MyService {
        public String work (String req) {
            return req + "/asyncwork";
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveStreamApplication5.class, args);
    }
}
