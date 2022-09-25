package chapter3;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SchedulerEx {

    /** Reactive Stream - Scheduler */
    // 단순한 구조에서는 그냥 main 스레드 하나에서 전부 다 실행된다.
    // 실제에서는 보통 publiser, subscriber를 따른 스레드를 둔다.
    // 스케줄러를 활용해서 request 부분을 다른 스레드에서 처리하도록 해보자.
    // --> 여기서는 subScribeOn(scheduler)를 통해서 내부에 어떤 스레드에서 해당 요청을 처리할지 지정이 가능하다.
    public static void main(String[] args) {

        System.out.println(getCurrentThread() + "START!");

        /** Publisher */
        Publisher<Integer> publisher = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    System.out.println(getCurrentThread() + "Request!");
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        /** SubscribeOn - subscribe가 실행되는 위치의 변경. 구독 및 데이터 생성을 시작하는 위치에 직접적인 영향을 준다. */
        // Subscribe on Publisher, 중간에 Operator를 하나 만들어두자. publisher와 subscriber를 연결해준다.
        Publisher<Integer> subOnPub = sub -> {
            // 하나의 worker만 가지는 싱글 스레드 생성
            // 그 이상의 작업이 들어오면 큐에 넣어두고 대기시킨다. 즉, 1개만 실행이 가능한 구조.
            ExecutorService es = Executors.newSingleThreadExecutor(
                    // 스레드 이름 지정해주기
                    r -> new Thread(r, "SubOnThread")
            );

            // publisher -> Operator 연결, 구독하도록 진행해준다.
            es.execute(() -> publisher.subscribe(sub));
            es.shutdown();

            /*
            결과)
            [main] START!
            [main] END!
            [pool-1-thread-1] Request!
            [pool-1-thread-1] onNext = 1
            [pool-1-thread-1] onNext = 2
            [pool-1-thread-1] onNext = 3
            [pool-1-thread-1] onNext = 4
            [pool-1-thread-1] onNext = 5
            [pool-1-thread-1] onComplete!
            [pool-1-thread-1] onSubscribe!

            확인해보면, 다음과 같이 생성부터 소모까지 별도의 스레드에서 비동기적으로 일어나는 걸 볼 수 있다.
            main 스레드의 blocking 없이, 작업을 수행할 수 있다는 장점이 있다!!
            */
        };

        /** publishOn - 이후의 작업을 다른 스레드에서 진행할 수 있도록 */
        // publisher on publisher - 생성은 빠른데 소모가 느린 경우 실행 작업을 다른 스레드에서 진행되도록 만들기
        Publisher<Integer> pubOnPub = sub -> {
            subOnPub.subscribe(new Subscriber<Integer>() {
                ExecutorService es = Executors.newSingleThreadExecutor(
                        r -> new Thread(r, "PubOnThread"));

                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                }

                @Override
                public void onNext(Integer integer) {
                    // 여기서 별개의 스레드를 통해서 실행되도록 만들어보자.
                    es.execute(() -> sub.onNext(integer));
                }

                @Override
                public void onError(Throwable t) {
                    es.execute(() -> sub.onError(t));
                    es.shutdown();
                }

                @Override
                public void onComplete() {
                    es.execute(() -> sub.onComplete());
                    es.shutdown();
                }
            });
            /*
            결과)
            [main] START!
            [main] END!
            [SubOnThread] Request!
            [SubOnThread] onSubscribe!
            [PubOnThread] onNext = 1
            [PubOnThread] onNext = 2
            [PubOnThread] onNext = 3
            [PubOnThread] onNext = 4
            [PubOnThread] onNext = 5
            [PubOnThread] onComplete!

            subscribe 및 데이터 생성까지는 main, 그 이후 작업(소모하는 작업)을 별도의 스레드에서 진행한다.
             */
        };

        /*
        subOnPub + pubOnPub 혼용 결과)
        [main] START!
        [main] END!
        [pool-2-thread-1] Request!
        [pool-2-thread-1] onSubscribe!
        [pool-1-thread-1] onNext = 1
        [pool-1-thread-1] onNext = 2
        [pool-1-thread-1] onNext = 3
        [pool-1-thread-1] onNext = 4
        [pool-1-thread-1] onNext = 5
        [pool-1-thread-1] onComplete!

        pool-2 -> subOnPub에서 진행된 것 (구독 및 데이터 생성)
        pool-1 -> 이후의 작업
        즉, 모두 별개의 작업에서 일어난다.

         */

        /** Subscriber */
        pubOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
                System.out.println(getCurrentThread() + "onSubscribe!");
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println(getCurrentThread() + "onNext = " + integer);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println(getCurrentThread() + "onError = " + t);
            }

            @Override
            public void onComplete() {
                System.out.println(getCurrentThread() + "onComplete!");
            }
        });

        System.out.println(getCurrentThread() + "END!");

    }

    /* 현재 스레드 출력 */
    private static String getCurrentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}
