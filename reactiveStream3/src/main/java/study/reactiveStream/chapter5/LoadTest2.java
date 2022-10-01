package study.reactiveStream.chapter5;

import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** 부하 테스트를 위한 코드 - Advanced */
public class LoadTest2 {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        // 100개의 요청을 동시에 생성하는 클라이언트 만들기
        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/hello-service-cf?idx={idx}";

        /** CyclicBarrier - 스레드 동기화 */
        // A CyclicBarrier supports an optional Runnable command that is run once per barrier point, after the last thread in the party arrives, but before any threads are released.
        // 마지막 스레드가 도착한 다음, 스레드 해제 전 barrier point당 한 번씩 실행되는 선택적인 Runnable command를 지원하는 도구.
        CyclicBarrier barrier = new CyclicBarrier(101);

        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int idx = counter.addAndGet(1);

                // await을 만나는 순간 코드는 blocking이 진행되며, 마지막 스레드까지 만나게 되었을 때 블록킹은 해제된다.
                // 아무튼, 스레드의 동기화를 할 수 있다는 점! - 100개가 모두 들어왔을 때 아래 코드가 실행되기 시작함.
                // 그래서 실제로 코드를 찍어보면 몇 번쨰 스레드가 먼저 들어오는지 알 수 없다. (뒤죽박죽임)
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

                // execute -> submit으로 바꾸면서 내부 파라미터를 runnable -> callable로 받을 수 있도록 하기 위해 의미없는 리턴 값 추가.
                // 이러면 내부에서 예외에 대한 처리를 해줄 수 있기 때문에 try-catch문 사용 대신 별도의 처리를 해주지 않아도 된다 (barrier.await() 부분 확인)
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

/*
    [RestTemplate]
    ReactiveStreamApplication3 + RemoteService (2초 정도 걸리는 작업) + LoadTest2 실행 결과
    - 이렇게 되었을 때 restTemplate getForObject의 경우 블록킹이다 보니 작업이 오래 걸린다.
    - 한 작업당 2초가 걸리기 때문에 해당 작업이 끝난 이후에 다른 작업을 받을 수 있다. (스레드 제한을 1개로 우선 걸어두었으니까...)
    --> 그러기 때문에, remoteService를 호출하는 것을 비동기적으로 바꾸어보자.

    [AsyncRestTemplate]
    - Non-Blocking 방식이기 때문에 이전처럼 요청이 오래 걸리지 않고 금방 완료된다.
    스레드가 1개여도 이렇게 Non-blocking 방식을 활용하면 100개의 요청을 바로 처리 완료할 수 있다.
    - 그러나, 이 방식 역시 비동기로는 하지만 비동기 작업을 수행하기 위한 스레드를 또 생성하기 때문에
    결과적으로 서버 자원 100개를 사용하는 것은 동일하다고 볼 수 있다.
    - 사실 이건 그래서 바람직한 방법이 아니다...!

    [AsyncRestTemplate] + with Netty
    - 이렇게 하면 작업 스레드를 추가적으로 생성하지 않고 (100개까지는 아님) 네티 호출을 위한 추가적인 스레드 생성만 진행한다. (1개...?)

    [Multi Request]
    - 2초 걸리는 작업을 내부에서 한 번 더 진행하였기 때문에 하나마다 총 4초 정도 걸린다.
    - non-blocking이기 때문에 총 작업 시간도 한 4초 정도 나온다.

    [Service Layer]
    - 서비스단을 추가했을 때도 역시 4초 정도 걸린다.
    사용된 스레드의 경우는 NIO servlet thread 1개, netty thread 1개, 우리가 커스텀한 myThread 1개.
    총 3개의 스레드로 3개의 비동기 작업 100개를 잘 수행해준다.
*/
