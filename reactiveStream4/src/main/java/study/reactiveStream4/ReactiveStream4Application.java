package study.reactiveStream4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static study.reactiveStream4.chapter8.PrintThreadName.getCurrentThread;

/** WebFlux 적용하기 */
@SpringBootApplication
@EnableAsync
public class ReactiveStream4Application {

	@RestController
	@SuppressWarnings("deprecation")
	public static class MyController {
		public static final String URL1 = "http://localhost:8081/service1?req={req}";
		public static final String URL2 = "http://localhost:8081/service2?req={req}";

		@Autowired
		MyService myService;

		/** WebClient */
		WebClient client = WebClient.create();
		@GetMapping("/hello-service")
		public Mono<String> hello(int idx) {
			/** Mono - Start! */
			// 기본적으로 param으로 넘긴 값이 Mono 안에 들어가는 느낌? 리스트랑 느낌은 비슷하다.
//			return Mono.just("Hello");

			/** Mono - basic (기본 요청 방법) */
//			return basicMono(idx);

			/** Mono - advanced (조금 더 다양한 요청) */
			return advancedMono(idx);
		}

		private Mono<String> advancedMono(int idx) {
			return client.get().uri(URL1, idx).exchange()
					.flatMap(cr1 -> cr1.bodyToMono(String.class))
					// 로그 용도 추가
					.doOnNext(c -> System.out.println(getCurrentThread() + c))
					// 그 다음 api 호출도 연결해주기
					// 여기까지 했을 때 리턴값은 Mono<ClientResponse>
					.flatMap(str1 -> client.get().uri(URL2, str1).exchange())
					.doOnNext(c -> System.out.println(getCurrentThread() + c))
					// 이를 다시 Mono<String> 형으로 꺼내주기
					.flatMap(cr2 -> cr2.bodyToMono(String.class))
					.doOnNext(c -> System.out.println(getCurrentThread() + c))
					// 서비스단의 리턴은 CompletableFuture<String>이기 때문에 Mono<String>으로 변환하는 작업이 필요하다.
					// CompletableFuture는 CompletionStage를 상속하였으며, Mono 내부에서 이를 Mono 타입으로 변환해준다.
					.flatMap(str2 -> Mono.fromCompletionStage(myService.work(str2)))
					.doOnNext(c -> System.out.println(getCurrentThread() + c));
			/*
			로그 확인)
			[reactor-http-nio-1] 1/service1
			[reactor-http-nio-1] org.springframework.web.reactive.function.client.DefaultClientResponse@4490a2f
			[reactor-http-nio-1] 1/service1/service2
			[myThreadPool-1] 1/service1/service2/asyncwork

			- 이런 식으로 요청이 하나의 스레드 안에서 동작하는 걸 확인할 수 있다.
			비동기 요청만 다른 스레드 요청에서 동작하도록 진행하였다.
		 	*/
		}

		private Mono<String> basicMono(int idx) {
			// ClientResponse 값이 기존의 ResponseEntity와 비슷하게 동작한다.
			// webClient는 builder 패턴으로 각 요청에 대한 옵션을 줄 수 있다.
			// 이 친구는 단순히 publisher이기 때문에 subscriber가 존재해야 실제 api 요청이 이루어진다.
			// 그러나, 리턴 타입이 Mono<> 타입이면 스프링은 알아서 subscribe()를 해주기 때문에 우리가 직접 해줄 필요는 없다.
			Mono<ClientResponse> res = client.get().uri(URL1, idx).exchange();

			// clientResponse.bodyToMono -> Mono<String>형으로 응닫값을 받음.
			// 이때, 그냥 map을 사용하면 Mono<Mono<String>>형으로 감싸서 오기 때문에 flatMap을 사용해준다.
			return res.flatMap(cr -> cr.bodyToMono(String.class));
		}
	}

	/** Service단 추가 */
	@Service
	public static class MyService {
		// 시간이 오래 걸리는 작업이라 가정하고, 별도의 스레드에서 작동하도록 변경
		@Async(value = "myThreadPool")
		public CompletableFuture<String> work (String req) {
			return CompletableFuture.completedFuture(req + "/asyncwork");

		}
	}

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
		System.setProperty("reactor.netty.ioWorkerCount", "1");
		System.setProperty("reactor.ipc.netty.pool.maxConnection", "2000");
		SpringApplication.run(ReactiveStream4Application.class, args);
	}
}
