package study.reactiveStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Netty를 기반으로 동작 (ReactiveStream) - 속도가 굉장히 빠르다! */
/** Mono의 동작 방식 알아보기 */
@SpringBootApplication
@RestController
@Slf4j
public class ReactiveStreamApplication {

	@GetMapping("/")
	Mono<String> hello() {
		/** 기본적으로 log 찍어보기 */
//		return basicLog();

		/** 자체적으로 로그 찍어봤을 때 어떤 게 먼저 실행될지 테스트 */
		log.info("Hello 1!");
//		Mono<String> mono = logDoOnNext();

		/** Mono.just와 실행 순서의 관계 */
//		Mono<String> mono = logWithJust(generateHello());

		/** supplier -> Mono가 실행되는 시점까지 지연하기 (람다식을 활용한 함수 형태로 변경하기) */
//		Mono<String> mono = FromSupplier(Mono.fromSupplier(() -> generateHello()));

		/** 명시적으로 subscribe를 해주었을 경우 */
//		Mono<String> mono = explicitSubscribe();

		/** Mono가 생성한 결과값 가져오기 */
		Mono<String> mono = monoBlock();

		log.info("Hello 2!");
		return mono;
	}

	private Mono<String> monoBlock() {
		String msg = generateHello();
		Mono<String> mono = Mono.just(msg).doOnNext(c -> log.info(c)).log();
		// 강의에서는 별다른 언급이 없이 넘어갔었는데, block을 진행하려면 springboot-start-web dependency를 추가해줘야 한다.
		// block() 메서드는 블록킹 방식으로 때문에, 현재 non-blocking 기반의 netty가 동작하고 있기 때문에 동작 자체가 안 된다...!
		// 이게 아마 버전업이 되면서 추가된 내용인 것 같다.

		String msg2 = mono.block(); // Mono가 생성한 결과값을 가져오기
		log.info("Message = {}" + msg2);
		// 그거랑 별개로, 강의에서 나온 결과를 보면 msg2 부분에 Hello Mono가 들어간다.
		// block() 메서드 내부에서 subscribe() 메서드가 동작하기 때문에 결과값을 꺼내서 줄 수 있는 것.
		// 단, 다시 subscribe를 하니깐 위의 작업이 오래 걸리는 작업이라면 성능샹 이슈가 있을 수 있기 때문에,
		// 리턴으로 mono를 해주지 않고 그냥 mono.just(msg2)를 해주는 게 낫다.
		// 이러면 새로운 Mono를 만들면서 넘겨주니까 결과값만 깔끔하게 넘겨줄 수 있다...!
		// 그래도 웬만하면 block을 안 쓰는 게 더 나을 것 같다.
		return mono;
	}

	private Mono<String> explicitSubscribe() {
		Mono<String> mono = Mono.fromSupplier(() -> generateHello())
				.doOnNext(c -> log.info(c))
				.log();
		mono.subscribe();

		/*
			결과)
			2022-10-02 23:47:33.634  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 1!
			2022-10-02 23:47:33.636  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onSubscribe([Fuseable] FluxPeekFuseable.PeekFuseableSubscriber)
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | request(unbounded)
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : method generateHello()
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello Mono
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onNext(Hello Mono)
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onComplete() #### HERE!!!!!
			2022-10-02 23:47:33.638  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 2! #### START
			2022-10-02 23:47:33.645  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onSubscribe([Fuseable] FluxPeekFuseable.PeekFuseableSubscriber)
			2022-10-02 23:47:33.646  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | request(unbounded)
			2022-10-02 23:47:33.646  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : method generateHello()
			2022-10-02 23:47:33.646  INFO 18179 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello Mono
			2022-10-02 23:47:33.646  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onNext(Hello Mono)
			2022-10-02 23:47:33.647  INFO 18179 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onComplete()

			- 우선, 명시적으로 subscribe를 하였기 때문에 Publisher에 담긴 게 정상 실행이 되어 HERE 윗부분까지 로그가 찍히게 된다.
			이후, Hello 2가 찍힌 다음, 스프링이 알아서 붙여준 subscribe 덕분에 한 번 더 프로세스가 찍히는 걸 볼 수 있다.
			== 즉, publisher는 여러 개의 subscriber를 가질 수 있다.

			- 또한, publisher가 공급하는 데이터의 타입에는 HOT, COLD source로 나뉘는데,
			데이터가 생성되고 접근되는 경우, 즉 어떤 subscriber가 접근하든 동일한 결과를 받는 데이터는 COLD source라고 하고
			실시간으로 데이터가 날라오는 경우, 받을 때마다 달라지는 경우를 HOT source라고 한다.
		 */
		return mono;
	}

	private Mono<String> FromSupplier() {
		// supplier = 파라미터는 없고 리턴값만 가지고 있음
		Mono<String> mono = Mono.fromSupplier(() -> generateHello())
				.doOnNext(c -> log.info(c))
				.log();

		/*
			결과)
			2022-10-02 23:40:26.586  INFO 17750 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 1!
			2022-10-02 23:40:26.587  INFO 17750 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 2!
			2022-10-02 23:40:26.595  INFO 17750 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onSubscribe([Fuseable] FluxPeekFuseable.PeekFuseableSubscriber)
			2022-10-02 23:40:26.597  INFO 17750 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | request(unbounded)
			2022-10-02 23:40:26.597  INFO 17750 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : method generateHello()
			2022-10-02 23:40:26.597  INFO 17750 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello Mono
			2022-10-02 23:40:26.597  INFO 17750 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onNext(Hello Mono)
			2022-10-02 23:40:26.600  INFO 17750 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onComplete()

			- 이렇게 되면 generateHello()가 먼저 실행되지 않고, Mono가 실행되는 시점까지 지연이 가능하다.
			결과를 보면 Hello 1, Hello 2가 찍히고, subscribe 후에 실행되는 것을 볼 수 있다.
		 */
		return mono;
	}

	private Mono<String> logWithJust(String data) {
		Mono<String> mono = Mono.just(data)
				.doOnNext(c -> log.info(c))
				.log();
		/*
			결과)
			2022-10-02 20:45:52.522  INFO 6478 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 1!
			2022-10-02 20:45:52.522  INFO 6478 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : method generateHello()
			2022-10-02 20:45:52.523  INFO 6478 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 2!
			2022-10-02 20:45:52.530  INFO 6478 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onSubscribe([Fuseable] FluxPeekFuseable.PeekFuseableSubscriber)
			2022-10-02 20:45:52.532  INFO 6478 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | request(unbounded)
			2022-10-02 20:45:52.532  INFO 6478 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello Mono
			2022-10-02 20:45:52.532  INFO 6478 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onNext(Hello Mono)
			2022-10-02 20:45:52.534  INFO 6478 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onComplete()

			- 이렇게 되면 이제 just 내부에 들어있는 값은 사전에 로딩이 되어야 하니까 먼저 실행이 되고,
			그 이후 subscribe를 진행하면서 로그를 찍게 된다.
		 */
		return mono;
	}

	private String generateHello() {
		log.info("method generateHello()");
		return "Hello Mono";
	}

	private static Mono<String> logDoOnNext() {
		Mono<String> mono = Mono.just("Hello WebFlux!")
				.doOnNext(c -> log.info(c))
				.log();
		return mono;
		/*
			결과)
			2022-10-02 20:39:00.230  INFO 6060 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 1!
			2022-10-02 20:39:00.231  INFO 6060 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello 2!
			2022-10-02 20:39:00.239  INFO 6060 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onSubscribe([Fuseable] FluxPeekFuseable.PeekFuseableSubscriber)
			2022-10-02 20:39:00.240  INFO 6060 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | request(unbounded)
			2022-10-02 20:39:00.241  INFO 6060 --- [ctor-http-nio-2] s.r.ReactiveStreamApplication            : Hello WebFlux!
			2022-10-02 20:39:00.241  INFO 6060 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onNext(Hello WebFlux!)
			2022-10-02 20:39:00.242  INFO 6060 --- [ctor-http-nio-2] reactor.Mono.PeekFuseable.1              : | onComplete()

			- 확인해보면 slf4j로 찍은 로그가 먼저 찍히고, 이후 모노에서의 로그가 찍힌 것을 확인할 수 있다.
			이 코드는 별도의 스레드를 통해 돌아가지 않았기 때문에 동기적인 코드다.
			기본적으로 publisher -> (operator) -> subscriber 데이터의 흐름에서 subscribe를 해야 플로우가 진행되기 때문에
			subscribe가 된 이후에 순차적으로 찍히는 게 아니라 로그가 찍히게 된다.
		 */
	}

	private static Mono<String> basicLog() {
		return Mono.just("Hello WebFlux!")
				// 로그 걸어주기
				.log();
		/*
			결과)
			2022-10-02 19:34:25.928  INFO 2339 --- [ctor-http-nio-2] reactor.Mono.Just.1                      : | onSubscribe([Synchronous Fuseable] Operators.ScalarSubscription)
			2022-10-02 19:34:25.930  INFO 2339 --- [ctor-http-nio-2] reactor.Mono.Just.1                      : | request(unbounded)
			2022-10-02 19:34:25.931  INFO 2339 --- [ctor-http-nio-2] reactor.Mono.Just.1                      : | onNext(Hello WebFlux!)
			2022-10-02 19:34:25.934  INFO 2339 --- [ctor-http-nio-2] reactor.Mono.Just.1                      : | onComplete()

			- subscribe의 경우 스프링이 알아서 해준 것.
			- onNext() -> onComplete()
		 */
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveStreamApplication.class, args);
	}

}
