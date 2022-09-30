package study.reactiveStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import study.reactiveStream.chapter4.PrintThreadName;

import java.util.concurrent.Future;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** SpringBoot Async Practice */
//@SpringBootApplication
@EnableAsync /** @EnableAsync를 추가해야 @Async 어노테이션이 동작한다. */
public class ReactiveStreamApplication {
	@Component
	public static class MyService {

		/** @Async + Future */
		// 비동기로 실행하도록하기 위해서 @Async 붙이기
		// 리턴값을 받아오게 하기 위해서 Future 사용하기
		@Async
		public Future<String> hello() throws InterruptedException {
			System.out.println(getCurrentThread() + "hello()");
			Thread.sleep(1000);
			// AsyncResult에 넣어서 리턴해준다.
			return new AsyncResult<>("Hello");
		}

		/** @Async + ListenableFuture */
		@Async
//		@Async(value = "tp")
		public ListenableFuture<String> hello2() throws InterruptedException {
			System.out.println(getCurrentThread() + "hello()");
			// 강의에서는 괜찮았는데 여기서 스레드 슬립 걸어버리면 인터럽트 걸려서 값이 정상적으로 안 온다...! 우선 지워둠.
//			Thread.sleep(1000);
			return new AsyncResult<>("Hello");
		}

		// 그러나, 기본적인 @Async의 경우 스레드 정책이 SimpleAsyncTaskExecutor를 사용하게 되는데,
		// 비동기 호출마다 새로운 스레드를 만들어서 사용하기 때문에 비효율적이다.
		// 그래서 직접 ThreadPoolTaskExecutor를 만들어서 사용하는 게 좋다.
		// @Bean으로 등록해주면 해당 스레드 정책을 따르게 된다.
		// 혹은 @Async를 통해 직접 지정해줄 수도 있다 (value =)

	}
	public static void main(String[] args) {
		/** try with Resource - 빈이 다 준비된 이후 종료되도록 */
		try(ConfigurableApplicationContext c
				= SpringApplication.run(ReactiveStreamApplication.class)) {
		}

	}
	/** 의존관계 주입 */
	@Autowired
	MyService myService;

	/** ThreadPoolTaskExecutor Custom */
	@Bean
	ThreadPoolTaskExecutor tp() {
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		// core만큼 차면 queue만큼 더 차고, 이것도 꽉 차면 max만큼 차게 된다.

		// 스레드 풀을 해당 개수만큼 생성하는데, 처음 요청이 들어왔을 때 poll size만큼 생성된다.
		te.setCorePoolSize(10);
		// core 스레드를 모두 사용 중일 때 큐에 만들어서 대기시킨다.
		te.setQueueCapacity(200);
		// 대기하는 작업이 큐에 꽉 차면 max만큼 더 생성한다.
		te.setMaxPoolSize(100);
		te.setThreadNamePrefix("Custom-Thread");

		te.initialize();
		return te;
	}

	/** Run After Bean Setting*/
	// 빈이 다 셋팅된 다음에 실행된다. (일종의 컨트롤러처럼 생각)
	@Bean
	ApplicationRunner run() {
		return args -> {
			System.out.println(getCurrentThread() + "run()");
			// 오랜 시간이 걸리는 작업의 경우 이렇게 비동기로 실행할 수 있는데,
			// 1) 작업이 끝나면 결과를 DB에 넣어두기
			// 2) 혹은 HttpSession에 future 값을 넣어두고, 값을 꺼내서 isDone이 true일 때 값을 사용하는 방식으로 진행하기


			/** Future  */
//			Future<String> result = myService.hello();
//			System.out.println(getCurrentThread() + "EXIT! " + result.isDone());
//			System.out.println(getCurrentThread() + "Result = " + result.get());

			/** ListenableFuture */
			ListenableFuture<String> result2 = myService.hello2();

			// ListenableFuture를 사용해서 success / error일 때의 콜백함수를 등록할 수 있다.
			result2.addCallback(s -> System.out.println(getCurrentThread() + s),
					e -> System.out.println(getCurrentThread() + e.getMessage()));

			System.out.println(getCurrentThread() + "EXIT!");
			};
		/*
		<Future>
		[main] run()
		[main] EXIT! false
		[task-1] hello()
		[main] Result = Hello

		- 이렇게 hello가 실행되기 전에 EXIT를 해주고, hello()는 별도의 스레드에서 실행되는 걸 볼 수 있다.
		그리고 작업이 끝난 이후에 AsyncResult에 넣어준 값이 리턴된다.

		<ListenableFuture>
		[main] run()
		[main] EXIT!
		[task-1] hello()
		[task-1] Hello
		- ListenableFuture를 사용하면 결과값을 받아오기 위해 하위 작업이 대기하지 않는다.
		파라미터로 넘겨준 callback method를 통해 성공 시와 실패 시 작업을 미리 지정을 해두기 때문!
		 */
	}

}
