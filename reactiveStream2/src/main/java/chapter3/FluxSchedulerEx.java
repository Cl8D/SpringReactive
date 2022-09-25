package chapter3;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FluxSchedulerEx {
    public static void main(String[] args) {
        System.out.println(getCurrentThread() + "START!");
        /** Reactor에서 제공하는 Flux 활용 */
        // 1 ~ 10 데이터 생성
//        simpleFluxEx();

        // pub, sub 혼용 => subscribe, request는 sub에서, onNext는 pub에서!

        /** interval 예제 */
        // 5초 동안 하나씩 값을 증가하며 출력을 해주는 것.
        intervalEx();

        /** interval Ex - 사용자 스레드 예제 */
//        userThreadExample();
    }

    private static void intervalEx() {
        Flux.interval(Duration.ofMillis(200))
                // 데이터를 10개만 받겠다고 지정이 가능하다!
                .take(10)
                .subscribe(s-> System.out.println(getCurrentThread()+s));

        try {
            // 이때, timeUnit을 걸어줘야 한다.
            // 위의 작업은 main이 아닌 다른 스레드에서 동작한다.
            // 이때, 위 작업은 유저 스레드가 아닌 데몬 스레드이기 때문에 main이 종료되면 종료되는 것이다!
            // 사용자가 만든 유저 스레드가 아닌, 데몬 스레드만 남아있으면 JVM이 shutdown을 시키기 때문에
            // timeout을 걸어두지 않으면 main이 종료되면 실행 결과를 볼 수 없는 것.
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void userThreadExample() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                System.out.println(getCurrentThread() + "Exception!");
            }
        });
        System.out.println(getCurrentThread() + "EXIT");
        /*
        결과)
        [main] START!
        [main] EXIT
        이런 식으로 time 조건이 지나고 나서도 EXIT가 찍힌다.
         */
    }

    private static void simpleFluxEx() {
        Flux.range(1, 10)
                // publishOn -> subscribe, request는 main, onNext는 pub에서
                .publishOn(Schedulers.newSingle("pub"))
                .log()
                // subscribeOn -> subscribe, request, onNext 작업 모두 별도의 스레드에서 동작한다.
                .subscribeOn((Scheduler) Schedulers.newSingle("sub").disposeGracefully())
                .subscribe(System.out::println);
    }

    private static String getCurrentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}
