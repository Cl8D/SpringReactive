package study.reactiveStream.chapter4;

import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** 부하 테스트를 위한 코드 */
public class LoadTest {
    /** Sync Servlet과 Async Servlet을 사용했을 때의 차이점을 알아보자. */
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        // 100개의 요청을 동시에 생성하는 클라이언트 만들기
        // 먼저, 100개의 스레드를 기본적으로 만들어두기.
        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/dr";

        // 시간 측정하기
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < 100; i++) {
            es.execute(() -> {
                int idx = counter.addAndGet(1);
                System.out.println(getCurrentThread() + "Thread " + idx);

                StopWatch sw = new StopWatch();
                sw.start();
                // 요청 진행
                rt.getForObject(url, String.class);
                sw.stop();

                // 각 스레드가 몇 초에 거쳐서 해당 작업을 완료했는지 출력
                // 확인 결과, 거의 2초 정도 걸린다.
                // [pool-1-thread-95] COMPLETED! [Thread 95], time = 2.228291157 sec
                System.out.println(getCurrentThread() + "COMPLETED! " +
                        "[Thread " + idx + "], time = " + sw.getTotalTimeSeconds() + " sec");
            });
        }

        es.shutdown();
        // 100초 정도는 기다리고 종료할 수 있도록 대기 작업 진행
        es.awaitTermination(100, TimeUnit.SECONDS);
        stopWatch.stop();

        // 확인해보면 2초 정도 걸린다.
        System.out.println(getCurrentThread() + "EXIT! Total Time = " + stopWatch.getTotalTimeSeconds() + " sec");
    }
}

/*
  스레드 풀을 20 정도로 하고 callable2()에 대해 부하 테스트 시 10초 정도 걸린다.
  그러나, callable()을 사용하면 2초가 걸린다. (이게 바로 비동기 서블릿 작업 덕분!)

  - Async Servlet은 클라이언트로 요청을 받은 다음에, 실제 작업은 작업 스레드 풀에 위임을 해주고,
  (작업 스레드는 사용되고 있지 않은 스레드를 사용하여 이러한 작업을 진행한다 - 내부에서 100개의 작업 스레드 사용)
  현재 서블릿 스레드는 다음 요청이 들어왔을 때 사용할 수 있도록 서블릿 스레드 풀에 빠르게 반환한다.

  - Sync Servlet의 경우 요청을 받은 스레드에서 실제로 작업까지 진행하기 때문에
  요청에 대한 응답을 반환하기 전까지는 새로운 요청을 처리할 수가 없다.

  사실 작업 스레드는 어차피 만들어지기 때문에 그렇게 큰 의미가 없다고 느낄 수도 있다.
  아무튼... 하나의 서블릿 스레드로도 여러 개의 요청을 받아 진행할 수 있다.

  그러나, 이렇게 작업 스레드를 만들지 않고도 작업을 처리할 수 있다...!
  -> 바로, DeferredResult를 사용하는 것!
 */

/*
    Deferred Result.
    - 클라이언트의 요청에 의해 지연되고 있는 HTTP Response를 나중에 써줄 수 있다.
    큰 특징은 별도의 워커 스레드를 만들어서 대기하지 않아도 처리할 수 있다는 점!

    - 클라이언트의 작업을 큐에 넣어두고, 나중에 응답을 써주는 느낌...!

    부하 테스트 적용 코드 (url 바꿔주기)
    - /dr 실행 (페이지 계속 대기중)
    - /dr/count 실행 (초기 1)
    - LoadTest 실행
    - /dr/count 실행 (101로 값 변경 - 스레드가 100개 더 생성되면서 큐의 사이즈 101로 증가
    -- 이 시점에 /dr은 계속 대기
    - /dr/event?message=wow 입력 시 /dr 페이지 로딩 완료 (Hello wow)
    -- 이는 곧, 큐에 담긴 DeferredResult 객체에 setResult로 결과를 반환하는 것이다.
    -- 이러면서 100개의 요청이 동시에 완료된다. (콘솔에도 동시에 시간 측정 결과가 나온다)
    - /dr/count로 확인해보면 0으로 변경된다 (모든 스레드가 작업을 완료하고 remove 진행됨)

 */