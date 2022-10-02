package study.reactiveStream4.chapter8;

import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static study.reactiveStream4.chapter8.PrintThreadName.getCurrentThread;

/** 부하 테스트를 위한 코드 */
public class LoadTest {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        // 100개의 요청을 동시에 생성하는 클라이언트 만들기
        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/hello-service?idx={idx}";

        CyclicBarrier barrier = new CyclicBarrier(101);

        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int idx = counter.addAndGet(1);
                barrier.await();

                System.out.println(getCurrentThread() + "Thread " + idx);

                StopWatch sw = new StopWatch();
                sw.start();

                // 요청 진행
                String result = rt.getForObject(url, String.class, idx);
                sw.stop();

                System.out.println(getCurrentThread() + "COMPLETED! " +
                        "[Thread " + idx + "], time = " + sw.getTotalTimeSeconds() +
                        " sec. result = " + result);

                return null;
            });
        }

        // 101개의 스레드가 도착했을 때 동시에 아래 코드로 넘어가게 된다.
        barrier.await();
        // 시간 측정하기
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        es.shutdown();
        // 100초 정도는 기다리고 종료할 수 있도록 대기 작업 진행
        es.awaitTermination(100, TimeUnit.SECONDS);

        stopWatch.stop();

        // 확인해보면 2초 정도 걸린다.
        System.out.println(getCurrentThread() + "EXIT! Total Time = " + stopWatch.getTotalTimeSeconds() + " sec");
    }
}
