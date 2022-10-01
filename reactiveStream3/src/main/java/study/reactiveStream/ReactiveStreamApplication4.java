package study.reactiveStream;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Consumer;
import java.util.function.Function;

/** 코드 리팩토링 진행하기 - How to Solve Callback Hell Problem? */
@SpringBootApplication
@EnableAsync
public class ReactiveStreamApplication4 {

    @RestController
    @SuppressWarnings("deprecation")
    public static class MyController {
        public static final String URL1 = "http://localhost:8082/service1?req={req}";
        public static final String URL2 = "http://localhost:8082/service2?req={req}";

        /** AsyncRestTemplate - With Netty : Single Thread */
        AsyncRestTemplate artn = new AsyncRestTemplate(
                new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @Autowired
        MyService myService;

        /** Callback Hell - Refactoring */
        @GetMapping("/hello-service-refactor")
        public DeferredResult<String> hello(int idx) {
            DeferredResult<String> dr = new DeferredResult<>();

            Completion
                    .from(artn.getForEntity(URL1, String.class, ("hello " + idx)))
                    // 중간 작업 진행
                    // andApply의 경우 무언가를 받아서 처리 후 리턴을 해줘야 한다.
                    .andApply(s -> artn.getForEntity(URL2, String.class, s.getBody()))
                    // myService에서 진행하는 비동기 호출 걸어주기
                    // 이때, 이 친구는 api 호출이 아니기 때문에 ResponseEntity<String>이 아닌 그냥 String을 리턴하게 된다.
                    // 그렇기 때문에 타입을 제네릭하게 설정하여 어떤 타입이든 받을 수 있도록 하자.
                    .andApply(s -> myService.work(s.getBody()))
                    // 에러는 공통으로 처리할 수 있도록 변경
                    // 만약 에러가 발생했다면 여기서 종료되고, 아니면 다음으로 넘어가도록 진행
                    .andError(e -> dr.setErrorResult(e.toString()))
                    // 이전에서 리턴된 작업의 결과를 andAccept에서 받아서 뒤의 람다식에 전달해주는 역할. (리턴 x)
                    .andAccept(s -> dr.setResult(s));

            return dr;
        }
    }

    /** Accept을 위한 클래스 */
    // accept 시 더 이상 반환형으로 넘겨줄 값이 없기 때문에 Void로 정의
    public static class AcceptCompletion<T> extends Completion<T, Void> {
        public Consumer<T> consumer;

        public AcceptCompletion(Consumer<T> con) {
            this.consumer = con;
        }

        // 다형성을 적용해서 부모 타입의 run을 상속받아 각 클래스에서 다른 동작을 할 수 있도록 만들기
        @Override
        public void run(T s) {
            consumer.accept(s);
        }
    }

    /** Apply를 위한 클래스 */
    public static class ApplyCompletion<T, R> extends Completion<T, R> {
        public Function<T, ListenableFuture<R>> fn;

        public ApplyCompletion(Function<T, ListenableFuture<R>> fn) {
            this.fn = fn;
        }

        @Override
        public void run(T s) {
            ListenableFuture<R> lf = fn.apply(s);
            // 다음 단계로 연결
            lf.addCallback(this::complete, this::error);
        }
    }

    /** Error를 위한 클래스 */
    public static class ErrorCompletion<R> extends Completion<R, R> {
        public Consumer<Throwable> consumer;

        public ErrorCompletion(Consumer<Throwable> con) {
            this.consumer = con;
        }

        @Override
        public void run(R s) {
            // 아무 일도 없으면 그냥 다음 단계에게 넘겨주기
            if (next != null) {
                next.run(s);
            }
        }

        @Override
        public void error(Throwable e) {
            // 어디선가 에러가 왔을 때 이를 처리해주기
            consumer.accept(e);
        }
    }


    /** 콜백 함수의 작업 완료 / 에러 발생 시 나오는 결과를 재정의하기 위한 클래스 */
    public static class Completion<T, R> {
        Completion next;

        // static으로 정의하였기 때문에 클래스 전체의 타입 파라미터 T, R과 관련이 없어서 따로 정의를 해줘야 한다.
        public static <T, R> Completion<T, R> from(ListenableFuture<R> lf) {
            Completion<T, R> c = new Completion<>();
            lf.addCallback(c::complete, c::error);
            return c;
        }

        public void error(Throwable e) {
            // 에러가 발생했다면 그 다음 것의 error를 호출하도록
            if (next != null)
                next.error(e);
        }

        // complete는 결과를 받아오는 역할이니까 R (result)
        public void complete(R s) {
            // 만약 이미 completion이 설정되었다면
            if (next != null) {
                // 다음의 completion에게 본인의 작업의 결과값을 넘겨주도록
                next.run(s);
            }
        }

        // 이전의 결과에서 받는 역할을 하니까 T (input)
        public void run(T s) {
        }

        /** Consumer Interface */
        // generic type의 매개변수를 받아서 특정 작업을 수행하는 경우 사용 - 데이터를 소비만 하고 아무것도 반환하지 않음
        // andAccept는 최종적으로 작업을 마무리하는 단계이기 때문에 R 타입을 받는다. (반환형)
        public void andAccept(Consumer<R> consumer) {
            Completion<R, Void> c = new AcceptCompletion<R>(consumer);
            // 이전의 completion의 next로 지정해주기
            this.next = c;
        }

        /** Funtion<T,R>, 입력과 출력 타입 필요 */
        // 중간 작업이기 때문에 입력으로 ResponseEntity<String>이, 출력으로 ListenableFuture<ResponseEntity<String>>이 사용된다.
        // chaining을 해주기 위해서 리턴으로 Completion을 준다.
        // andApply는 다음의 completion에게 넘겨줘야 하기 때문에 R 타입, 그리고 출력은 어떻게 될지 모르니까
        // 메서드 레벨에서 새로운 제네릭 타입을 생성해서 넘겨줘야 한다.
        public <V> Completion<R, V> andApply(Function<R, ListenableFuture<V>> fn) {
            Completion<R, V> c = new ApplyCompletion<>(fn);
            this.next = c;
            return c;
        }

        public Completion<R, R> andError(Consumer<Throwable> consumer) {
            // 에러가 발생하면 작업 종료
            // 에러가 없으면 작업 그대로 진행 -> 값을 그대로 넘겨주면 되니까 R 타입
            Completion<R, R> c = new ErrorCompletion<>(consumer);
            this.next = c;
            return c;
        }
    }

    /** Service단 추가 */
    @Service
    public static class MyService {
        @Async(value = "myThreadPool2")
        public ListenableFuture<String> work (String req) {
            return new AsyncResult<>(req + "/asyncwork");
        }
    }

    /** 스레드 개수 제한해주기 */
    @Bean
    public ThreadPoolTaskExecutor myThreadPool2() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(10);
        te.initialize();
        return te;
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveStreamApplication4.class, args);
    }
}
