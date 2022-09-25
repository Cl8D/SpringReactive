package chapter2;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericPubSub {
    /** 이전의 PubSub을 조금 더 generic하게 변환하기 */
    public static void main(String[] args) {
        Publisher<Integer> publisher = iterPub(Stream.iterate(1, val -> val + 1).limit(10).collect(Collectors.toList()));

        // 타입을 조금 더 제네릭하게 받을 수 있도록 변경하기
        // 다양한 방식으로 활용이 가능해진다.
//        Publisher<String> mapPub = mapPub(publisher, s -> "[" + s + "]");

//        Publisher<Integer> mapPub2 = mapPub(mapPub, s -> -s);

//        Publisher<Integer> subPub = sumPub(mapPub);

        Publisher<StringBuilder> reducePub = reducePub(publisher, new StringBuilder(),
                (a, b) -> a.append(b).append(","));


        reducePub.subscribe(logSub());
        /*
        결과 - mapPub
        onSubscribe!
        onNext = [1]
        onNext = [2]
        onNext = [3]
        onNext = [4]
        onNext = [5]
        onNext = [6]
        onNext = [7]
        onNext = [8]
        onNext = [9]
        onNext = [10]
        onComplete!
         */

        /*
        결과 - reducePub
        onSubscribe!
        onNext = 1,2,3,4,5,6,7,8,9,10,
        onComplete!
         */
    }

    /** 이전의 계산값을 활용하여 합을 구하는 publisher를 생성해준다. */
    private static<T, R> Publisher<R> reducePub(Publisher<T> publisher, R start, BiFunction<R, T, R> biFunc) {
        return sub -> {
            publisher.subscribe(new DelegateGenSub<T, R>(sub) {
                R result = start;

                @Override
                public void onNext(T integer) {
                    result = biFunc.apply(result, integer);
                }

                @Override
                public void onComplete() {
                    sub.onNext(result);
                    sub.onComplete();
                }
            });
        };
    }

    /** 데이터의 합을 구하는 publisher를 생성해준다. */
    private static <T, R> Publisher<R> sumPub(Publisher<T> publisher) {
        return sub -> publisher.subscribe(new DelegateGenSub<T, R>(sub) {
            int sum = 0;

            @Override
            public void onNext(T integer) {
                sum += (int) integer;
            }

            @Override
            public void onComplete() {
                // subscriber에게 합을 건네주기
                sub.onNext(sum);
                sub.onComplete();
            }
        });
    }

    /** 데이터를 가공해주는 publisher를 만들어준다. */
    /** 이때, input과 return type이 다른 상황에서 제네릭하게 받을 수 있도록 해보자.*/
    private static <T, R> Publisher<R> mapPub(Publisher<T> publisher, Function<T, R> func) {
        return sub -> publisher.subscribe(new DelegateGenSub<T, R>(sub) {
            @Override
            public void onNext(T integer) {
                sub.onNext(func.apply(integer));
            }
        });
    }

    /** log를 찍어주는 subscriber이다. */
    private static<T> Subscriber<T> logSub() {
        return new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe!");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T integer) {
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
    private static<T> Publisher<T> iterPub(List<T> iter) {
        return sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    try {
                        iter.forEach(sub::onNext);
                        sub.onComplete();
                    } catch (Throwable t) {
                        sub.onError(t);
                    }
                }

                @Override
                public void cancel() {
                }
            });
        };
    }

}
