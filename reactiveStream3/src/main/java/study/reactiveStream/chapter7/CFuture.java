package study.reactiveStream.chapter7;

import java.util.concurrent.*;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** CompletableFuture Practice */
public class CFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 비동기 작업의 결과를 간단하게 만들어낼 수 있다!
        // CompletableFuture 리스트의 모든 값이 완료될 때까지 기다릴지, 아니면 하나의 값만 완료되길 기다릴지 선택이 가능하다.

        /** Completable Basic - 작업이 완료된 상태 / 혹은 작업 미완료 선언 후 추후 진행 */
        // 아래의 코드는, 이미 '작업이 완료된 상태로' Future Object를 만들어주기 때문에 .get을 해도 바로 리턴이 된다.
//        basicCompletable();

        // 작업이 완료되지 않은 상태로 설정 후 추후 진행
//        basicCompletable2();

        /** runAsync() - 비동기 작업 진행 */
//        runAsync();

        /** runAsync() + thenRun() - 연쇄적으로 다음 작업 진행*/
//        runAsyncThenRun();

        /** supplyAsync() + thenApply() + thenAccept() - 이전 작업의 리턴값을 다음 작업으로 넘겨주기 가능 */
//        supplyAsync();

        /** supplyAsync() + thenCompose() + thenAccept() - CompletableFuture.completedFuture 타입으로 반환  */
//        thenCompose();

        /* 찾아봤는데 thenApply vs thenSupply()
        * 동기적으로 매핑할 때는 thenApply를 사용하고, 비동기적으로 매핑할 때는 thenCompose()를 사용한다고 한다. (어렵당...)
        * 으음... 흔히 map이랑 flatmap과 비슷하다고 말을 하는데,
        * map()의 경우 단일 요소로 리턴되기 때문에 2번의 체이닝이 필요하지만,
        * flatMap()의 경우 결과가 모두 단일 스트림 값으로 나오기 때문에 1번의 체이닝으로 출력이 가능하다.
        * */

        /** exceptionally() - 예외 처리 */
//        exceptionally();

        /** thenApplyAsync - 다른 스레드에서 작업하고 싶을 때 사용 */
        // 어떤 스레드 전략을 사용할 것인지 명시적으로 선언
//        ExecutorService es = Executors.newFixedThreadPool(10);
//        thenApplyAsync(es);

        System.out.println(getCurrentThread() + "EXIT!");

        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);

    }

    private static void thenApplyAsync(ExecutorService es) {
        CompletableFuture
                .supplyAsync(() -> {
                    System.out.println(getCurrentThread() + "supplyAsync!");
                    return 1;
                }).thenCompose((res) -> {
                    System.out.println(getCurrentThread() + "thenCompose, res = " + res);
                    return CompletableFuture.completedFuture(res + 1);
                }).thenApplyAsync((res) -> {
                    // 해당 작업은 다른 스레드에서 작업하고 싶을 때 사용
                    System.out.println(getCurrentThread() + "thenApplyAsync!, res = " + res);
                    return res * 3;
                }, es)
                .exceptionally(
                        // 예외가 발생하면 그냥 -10이라는 값을 넘기기. 즉, 예외를 복구하는 느낌?
                        e -> -10)
                .thenAccept((res) -> {
                    // thenAccept는 파라미터는 있고 리턴값이 있는 형식이다. 작업의 마무리.
                    System.out.println(getCurrentThread() + "thenAccept, res = " + res);
                });

        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] supplyAsync!
            [ForkJoinPool.commonPool-worker-1] thenCompose, res = 1
            [pool-1-thread-1] thenApplyAsync!, res = 2
            [pool-1-thread-1] thenAccept, res = 6

            - 하위 작업부터 새로운 스레드에서 실행되는 것을 볼 수 있다.
         */
    }

    private static void exceptionally() {
        CompletableFuture
                .supplyAsync(() -> {
                    System.out.println(getCurrentThread() + "supplyAsync!");
                    if (1==1) {
                        throw new RuntimeException();
                    }
                    return 1;
                }).thenCompose((res) -> {
                    // CompletableFuture를 '무조건' 반환하도록 하려면 thenCompose()를 사용한다.
                    System.out.println(getCurrentThread() + "thenCompose, res = " + res);
                    return CompletableFuture.completedFuture(res + 1);
                }).exceptionally(
                        // 예외가 발생하면 그냥 -10이라는 값을 넘기기. 즉, 예외를 복구하는 느낌?
                        e -> -10)
                .thenAccept((res) -> {
                    // thenAccept는 파라미터는 있고 리턴값이 있는 형식이다. 작업의 마무리.
                    System.out.println(getCurrentThread() + "thenAccept, res = " + res);
                });
        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] supplyAsync!
            [ForkJoinPool.commonPool-worker-1] thenAccept, res = -10

            - 강제로 결과값에 -10을 리턴하도록 진행
         */
    }

    private static void thenCompose() {
        CompletableFuture
                .supplyAsync(() -> {
                    System.out.println(getCurrentThread() + "supplyAsync!");
                    return 1;
                }).thenCompose((res) -> {
                    // CompletableFuture를 '무조건' 반환하도록 하려면 thenCompose()를 사용한다.
                    System.out.println(getCurrentThread() + "thenCompose, res = " + res);
                    return CompletableFuture.completedFuture(res + 1);
                }).thenAccept((res) -> {
                    // thenAccept는 파라미터는 있고 리턴값이 있는 형식이다. 작업의 마무리.
                    System.out.println(getCurrentThread() + "thenAccept, res = " + res);
                });

        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] supplyAsync!
            [ForkJoinPool.commonPool-worker-1] thenCompose, res = 1
            [ForkJoinPool.commonPool-worker-1] thenAccept, res = 2
         */
    }

    private static void supplyAsync() {
        // 기존의 runAsync()는 파라미터로 runnable를 받기 때문에 리턴값을 줄 수가 없다.
        // 리턴값을 얻기 위해서는 supplyAsync를 사용해야 한다!
        CompletableFuture
                .supplyAsync(() -> {
                    System.out.println(getCurrentThread() + "supplyAsync!");
                    return 1;
                }).thenApply((res) -> {
                    // thenApply를 사용하면 앞의 비동기 작업에서 리턴된 값을 사용할 수 있다.
                    System.out.println(getCurrentThread() + "thenApply, res = " + res);
                    return res + 1;
                }).thenAccept((res) -> {
                    // thenAccept는 파라미터는 있고 리턴값이 있는 형식이다. 작업의 마무리.
                    System.out.println(getCurrentThread() + "thenAccept, res = " + res);
                });

        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] supplyAsync!
            [ForkJoinPool.commonPool-worker-1] thenApply, res = 1
            [ForkJoinPool.commonPool-worker-1] thenAccept, res = 2

            - 이런 식으로 이전의 결과값을 가공하여 또 넘겨주는 작업을 진행할 수 있다.
         */
    }

    private static void runAsyncThenRun() {
        // 혹은 이런 식으로 그 다음 작업을 연쇄적으로 선언해줄 수도 있다.
        CompletableFuture
                .runAsync(() -> {
                    System.out.println(getCurrentThread() + "runAsync!");
                }).thenRun(() -> {
                    System.out.println(getCurrentThread() + "thenAsync1!");
                }).thenRun(() -> {
                    System.out.println(getCurrentThread() + "thenAsync2!");
                });
        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] runAsync!
            [ForkJoinPool.commonPool-worker-1] thenAsync1!
            [ForkJoinPool.commonPool-worker-1] thenAsync2!

            - 이런 식으로 동일한 스레드에서 연쇄적으로 실행된다.
         */
    }

    private static void runAsync() {
        CompletableFuture.runAsync(() -> {
            System.out.println(getCurrentThread() + "runAsync!");
        });
        /*
            결과)
            [main] EXIT!
            [ForkJoinPool.commonPool-worker-1] runAsync!

            - main이 먼저 실행되고, 다른 스레드에서 작업이 실행되는 걸 볼 수 있다.
            이때, pool을 아무것도 설정하지 않으면 디폴트로 ForkJoinPool.commonPool에서 스레드를 할당받는다.
         */
    }

    private static void basicCompletable2() throws InterruptedException, ExecutionException {
        // 혹은, 아래의 단계에서는 작업이 완료되지 않은 채로 설정을 해두지만
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        // 다른 스레드에서 complete를 실행하여 해당 작업을 완료시킬 수도 있다.
        cf.complete(2);

        // 혹은 예외가 발생했을 때 명시적으로 선언해줄 수도 있다.
        cf.completeExceptionally(new RuntimeException());

        // 음... 아무튼 결과값을 명시적으로 써줄 수 있다는 게 장점인 것 같다!
        System.out.println(cf.get());
    }

    private static void basicCompletable() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> cf
                = CompletableFuture.completedFuture(1);
        System.out.println(cf.get());
    }
}
