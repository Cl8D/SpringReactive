package chapter1;

import java.util.Arrays;
import java.util.Iterator;

import static java.util.concurrent.Flow.*;

/**
 * publisher - subscriber 동작 구조 제작
 */
public class PubSub {
    // observer 패턴과 유사하다.
    public static void main(String[] args) {
        // publisher is a 'provider' unbounded number of sequence elements.
        // and, demand received from Subscriber.

        // ** Publisher.subscribe following this protocol. **
        // onSubscribe onNext* (onError | onComplete)?
        // obSubscribe -> required, 필수로 호출
        // onNext -> 0~inf번까지 호출 가능
        // onError | onComplete -> 둘 중에 하나 호출

        Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);

        /* Publisher, 데이터를 누구에게 줄 것인지. */
        Publisher publisher = new Publisher() {
            @Override
            public void subscribe(Subscriber subscriber) {
                // 파라미터에 subscription을 넣어줄 수 있다. (객체를 만들어서 넘겨줌)
                // 구독이라는 정보를 가지고 있는 객체로, publisher, subscriber가 서로 참고할 수 있음
                // 둘 사이에서 정보의 속도 차이든, 뭐든 중개자 역할이라고 볼 수 있다.
                subscriber.onSubscribe(new Subscription() {
                    // subscribe를 하는 순간에 iterator를 새롭게 하나 생성
                    Iterator<Integer> iterator = iter.iterator();

                    @Override
                    public void request(long n) {
                        // n만큼 동작할 수 있도록 하기 위해서 조건 도입
                        while(n-- > 0) {
                            if (iterator.hasNext()) {
                                subscriber.onNext(iterator.next());
                            } else {
                                subscriber.onComplete();
                                break;
                            }
                        }
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
            // 간단하게 임의의 버퍼 사이즈 지정
            int bufferSize = 2;

            @Override // required, 반드시 호출되어야 한다. 가장 먼저 호출!
            public void onSubscribe(Subscription subscription) {
                System.out.println("onSubscribe!");
                // 데이터를 전부 다 달라는 의미
//                subscription.request(Long.MAX_VALUE);
                this.subscription = subscription;
                this.subscription.request(2);
            }

            // 다음 요청을 받을 준비가 되었는지
            @Override
            public void onNext(Integer item) {
                // 버퍼 사이즈가 0보다 작아지면 그 다음 데이터 요청
                if(--bufferSize <= 0) {
                    System.out.println("onNext = " + item);
                    bufferSize = 2;
                    // 그 다음의 2개 요청하기
                    subscription.request(2);
                }
                System.out.println("bufferSize = " + bufferSize);
                /*
                onSubscribe!
                bufferSize = 1
                onNext = 2
                bufferSize = 1
                onNext = 4
                bufferSize = 1
                onComplete!
                bufferSize = 1
                bufferSize = 1
                 */
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

    }
}