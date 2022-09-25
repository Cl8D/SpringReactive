package chapter2;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PubSub {
    public static void main(String[] args) {
        /** Reactive Stream에서 제공하는 Publisher - Subscriber를 사용한다. (Gradle Import) */

        // 1 ~ 10까지의 데이터를 생성해서 넘겨주기
        Publisher<Integer> publisher = iterPub(Stream.iterate(1, val -> val + 1).limit(10).collect(Collectors.toList()));

        // 중간에 데이터를 가공해서 넘겨주는 작업 진행
        // subscriber 입장에서도 데이터를 가공해주는 친구도 publisher라고 볼 수 있다.
        Publisher<Integer> mapPub = mapPub(publisher, s -> s * 10);

        // 2번째 mapPub 생성
//        Publisher<Integer> mapPub2 = mapPub(mapPub, s -> -s);

        // 3번째 생성 - 합계 계산
//        Publisher<Integer> subPub = sumPub(mapPub);

        // 4번째 - 조금 더 제네릭하게 합계 계산
        // 두 번째 원소는 초기값.
        // 우리가 원하는 계산 식을 세 번째 원소로 전달해줄 수 있다.
        Publisher<Integer> reducePub = reducePub(mapPub, 0, (a, b) -> a + b);

        /* Flow : Publisher -> Data1 -> mapPub -> Data2 -> Subscriber */
        /*                           <- subscribe(logSub)             */
        /*                           <- onSubscribe(s)                */
        /*                           <- onNext                        */
        /*                           <- onNext                        */
        /*                           <- onComplete                    */
        reducePub.subscribe(logSub());
    }

    /** 이전의 계산값을 활용하여 합을 구하는 publisher를 생성해준다. */
    private static Publisher<Integer> reducePub(Publisher<Integer> publisher, int start, BiFunction<Integer, Integer, Integer> biFunc) {
        return sub -> {
            publisher.subscribe(new DelegateSub(sub) {
                int result = start;
                @Override
                public void onNext(Integer integer) {
                    // apply의 두 가지 파라미터
                    // 첫 번째는 현재의 계산값, 두 번째는 계산할 때 사용될 값.
                    // 현재 수식이 (a, b) -> a+b니까, result+i의 값이 새롭게 계산되어 다음의 start로 들어오게 되는 것.
                    result = biFunc.apply(result, integer);
                }

                @Override
                public void onComplete() {
                    // 마지막에 전달.
                    sub.onNext(result);
                    sub.onComplete();
                    /*
                    결과)
                    onSubscribe!
                    onNext = 550
                    onComplete!
                     */
                }
            });
        };
    }

    /** 데이터의 합을 구하는 publisher를 생성해준다. */
    private static Publisher<Integer> sumPub(Publisher<Integer> publisher) {
        return sub -> publisher.subscribe(new DelegateSub(sub) {
            int sum = 0;

            @Override
            public void onNext(Integer integer) {
                sum += integer;
            }

            // 데이터를 다 합해준 이후에 합한 값들을 전달해주기
            @Override
            public void onComplete() {
                // subscriber에게 합을 건네주기
                sub.onNext(sum);
                sub.onComplete();
                /*
                결과)
                onSubscribe!
                onNext = 550
                onComplete!
                 */
            }
        });
    }

    /** 데이터를 가공해주는 publisher를 만들어준다. */
    private static Publisher<Integer> mapPub(Publisher<Integer> publisher, Function<Integer, Integer> func) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                // 여기서 sub는 logSub
                publisher.subscribe(new DelegateSub(sub) {
                    @Override
                    public void onNext(Integer integer) {
                        sub.onNext(func.apply(integer));
                    }
                });
            }
        };
    }

    /** log를 찍어주는 subscriber이다. */
    private static Subscriber<Integer> logSub() {
        return new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe!");
                // 데이터를 몇 개 달라고 할지 설정하기 - 여기서는 무제한으로!
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("onNext = " + integer);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError = " + t);
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete!");
            }
        };
    }

    /** 가장 윗단계의 Publisher다. */
    private static Publisher<Integer> iterPub(List<Integer> iter) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                // subscribe에게 구독 정보 (subscription) 던져주기
                // 구독이 일어났을 때의 행위를 정의한다고 생각하기
                sub.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        try {
                            // subscriber에게 iter에 있는 값을 순회하면서 하나씩 보내주기.
                            iter.forEach(sub::onNext);
                            // 완료했다는 신호 보내주기
                            sub.onComplete();
                        } catch (Throwable t) {
                            // 에러가 발생했다면 던져주기
                            sub.onError(t);
                        }
                    }

                    @Override
                    public void cancel() {
                        // cancel 되었을 때 하는 작업.
                        // publisher에게 데이터를 보내지 말아달라고 요청할 때 사용함 (여기서 처리한다는 것!)
                        // subscription을 통해서 cancel 가능
                    }
                });
            }
        };
    }

}
