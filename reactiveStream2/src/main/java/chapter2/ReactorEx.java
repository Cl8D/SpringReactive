package chapter2;

import reactor.core.publisher.Flux;

public class ReactorEx {
    /** reactor에서 제공하는 Flux Example  */
    /** 연산자에 대한 자세한 참고 => https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#reduce-A-java.util.function.BiFunction- */

    public static void main(String[] args) {
        // flux -> publisher의 일종.
        Flux.<Integer>create(e -> {
                    // subscription에서 썼던 메서드들을 간단하게 사용할 수 있다. (비슷한 역할)
                    e.next(1); // onNext라고 생각해주면 된다.
                    e.next(2);
                    e.next(3);
                    e.complete();
                })
                .log()
                .map(s -> s*10) // map 사용 시 10, 20, 30 이렇게!
                .reduce(0, (a, b) -> a+b) // reduce 역시 가능하다. (결과는 60)
                .subscribe(System.out::println);
        // .subscribe 시 인자로 consumer를 받을 수 있는데, 이는 onNext의 값을 받아볼 수 있다.

        /*
            log() 실행 시
            [ INFO] (main) onSubscribe(FluxCreate.BufferAsyncSink)
            [ INFO] (main) request(unbounded) // unbounded, 즉 제한 없이 가지고 있는 걸 전부 보내달라는 것
            [ INFO] (main) onNext(1)
            1
            [ INFO] (main) onNext(2)
            2
            [ INFO] (main) onNext(3)
            3
            [ INFO] (main) onComplete()
         */
    }
}