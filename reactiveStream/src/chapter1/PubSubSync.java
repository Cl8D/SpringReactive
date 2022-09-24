package chapter1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Flow.*;

/**
 * publisher - subscriber 동작 구조 제작
 * 비동기 처리
 */
public class PubSubSync {
    public static void main(String[] args) throws InterruptedException {
        Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);
        ExecutorService es = Executors.newCachedThreadPool();

        /* Publisher */
        Publisher publisher = new Publisher() {
            @Override
            public void subscribe(Subscriber subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    Iterator<Integer> iterator = iter.iterator();

                    @Override
                    public void request(long n) {
                        // Future는 비동기적으로 시작된 작업에 대한 결과 완료 여부, 결과를 제공해준다.
                        // future의 경우 subscriber가 중간에 작업을 cancel 했을 때 interrupt를 날려줄 수도 있다.
//                        Future<?> f = es.submit(() -> {
//                            while(n-- > 0) {
//                                if (iterator.hasNext()) {
//                                    // 여기서 무조건 싱글 스레드 보장. 하나의 스레드에서 타고 들어간다.
//                                    subscriber.onNext(iterator.next());
//                                } else {
//                                    subscriber.onComplete();
//                                    break;
//                                }
//                            }
//                        });

                        // 별도의 스레드에서 실행할 수 있도록 해주기!
                        es.execute(() -> {
                            int i = 0;
                            while(i++ < n) {
                                if (iterator.hasNext()) {
                                    // 여기서 무조건 싱글 스레드 보장. 하나의 스레드에서 타고 들어간다.
                                    subscriber.onNext(iterator.next());
                                } else {
                                    subscriber.onComplete();
                                    break;
                                }
                            }
                        });
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        };

        /* Subscriber */
        Subscriber<Integer> subscriber = new Subscriber<Integer>() {
            Subscription subscription;

            @Override // required, 반드시 호출되어야 한다. 가장 먼저 호출!
            public void onSubscribe(Subscription subscription) {
                // main. subscribe를 한 스레드에서 동작한다.
                System.out.println(Thread.currentThread().getName());
                System.out.println("onSubscribe!");
                subscription.request(1);
                this.subscription = subscription;
            }

            // 다음 요청을 받을 준비가 되었는지
            @Override
            public void onNext(Integer item) {
                // pool-1-thread-1, pool-1-thread-2 이렇게 별개의 스레드에서 동작한다.
                System.out.println(Thread.currentThread().getName());
                System.out.println("onNext = " + item);
                subscription.request(1);
            }

            // 에러가 났을 때 어떻게 처리할지
            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError!");
            }

            // 완료되었을 때 어떻게 처리할지
            @Override
            public void onComplete() {
                System.out.println("onComplete!");
            }
        };

        // publisher에게 subscriber를 등록해주기
        publisher.subscribe(subscriber);
        es.awaitTermination(10, TimeUnit.SECONDS);
        es.shutdown();

    }

    /*
    main
    onSubscribe!
    pool-1-thread-1
    onNext = 1
    pool-1-thread-2
    onNext = 2
    pool-1-thread-1
    onNext = 3
    pool-1-thread-2
    onNext = 4
    pool-1-thread-1
    onNext = 5
    onComplete!
    */
}
