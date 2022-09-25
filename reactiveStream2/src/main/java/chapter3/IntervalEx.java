package chapter3;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntervalEx {
    /** Interval Example */
    public static void main(String[] args) {
        System.out.println(getCurrentThread() + "START!");

        /** Publisher */
        Publisher<Integer> publisher = sub -> {
            sub.onSubscribe(new Subscription() {
                int number = 0;
                boolean isCanceled;

                @Override
                public void request(long n) {
                    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                    // 어떠한 작업을 일정 시간의 간격을 두고 계속 실행하도록 할 수 있다.
                    // param -> runnable, initialDay, period, timeUnit
                    // 초기 딜레이값, 사이사이 기간, 단위

                    // 종료하기 전까지 계속 데이터를 보내는 코드
                    // 일정 수만큼 데이터를 보내면 멈추는 작업을 진행해보자.
                    ses.scheduleAtFixedRate(() -> {
                        // 10개 이상이 되면 isCanceled=true가 되니까 작업 종료
                        if (isCanceled) {
                            ses.shutdown();
                            return;
                        }
                        sub.onNext(number++);
                    }, 0, 300, TimeUnit.MILLISECONDS);
                }

                @Override
                public void cancel() {
                    isCanceled = true;
                }
            });
        };

        /** Flux의 take와 동일한 작업을 하는 Publisher 구현 */
        Publisher<Integer> takePub = sub -> {
            publisher.subscribe(new Subscriber<Integer>() {
                int counter = 0;
                Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                    this.subscription = s;
                }

                @Override
                public void onNext(Integer integer) {
                    sub.onNext(integer);
                    // 10개 이상이 되면 cancel 작업 진행하기
                    if (++counter >= 10) {
                        subscription.cancel(); // cancel 요청
                        // 음... subscribe를 통해서 약간 원하는 시점에서 데이터를 그만 달라고 전달을 해줄 수 있다는 것.
                    }

                }

                @Override
                public void onError(Throwable t) {
                    sub.onError(t);
                }

                @Override
                public void onComplete() {
                    sub.onComplete();
                }
            });
        };

        /** Subscriber */
        takePub.subscribe(new Subscriber<Integer>() {
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

        System.out.println(getCurrentThread() + "EXIT!");

        /*
        결과)
        [main] START!
        [main] onSubscribe!
        [main] EXIT!
        [pool-1-thread-1] onNext = 0
        [pool-1-thread-1] onNext = 1
        [pool-1-thread-1] onNext = 2
        [pool-1-thread-1] onNext = 3
        [pool-1-thread-1] onNext = 4
        [pool-1-thread-1] onNext = 5
        [pool-1-thread-1] onNext = 6
        [pool-1-thread-1] onNext = 7
        [pool-1-thread-1] onNext = 8
        [pool-1-thread-1] onNext = 9
         */
    }

    private static String getCurrentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}
