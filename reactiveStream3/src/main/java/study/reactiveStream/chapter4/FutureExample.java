package study.reactiveStream.chapter4;

import java.util.concurrent.*;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** Java Future Practice */
public class FutureExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // A Future represents the result of an asynchronous computation.
        // 비동기적인 작업에 대한 결과를 나타낸다.

        // newCachedThreadPool()
        // Creates a thread pool that creates new threads as needed, but will reuse previously constructed threads when they are available.
        // 요청이 들어오면 스레드를 만들지만, 기본적으로 재사용이 가능하기 때문에 이전에 반납된 스레드가 있으면 해당 스레드를 리턴해준다.
        ExecutorService es = Executors.newCachedThreadPool();

        /** execute Practice - 별도의 스레드에서 진행 */
//        executePractice(es);

        /** submit Practice - 리턴값 받기 */
//        Future<String> value = submitPractice(es);

        /** FutureTask Practice 1 - 리턴으로 Future Object 받기 */
//        FutureTask<String> value = futurePractice1();

        /** FutureTask Practice 2 - 작업 완료 시 진행할 기능 선언*/
//        FutureTask<String> value = futurePractice2();
//        es.execute(value);

        /*
        단순히 Future의 경우 비동기 작업의 생성 및 실행을 동시에 하지만,
        FutureTask의 경우 비동기 작업의 생성과 실행을 분리하여 진행이 가능하다.
         */


        // 실행이 완료되었는지 확인 - 이거는 결과값을 받아올 때까지 기다리지 않고 바로 실행된다.
//        System.out.println(getCurrentThread() + "isDone? = " + value.isDone());
//        System.out.println(getCurrentThread() + "Future = " + value.get());

        System.out.println(getCurrentThread() + "EXIT!");
        es.shutdown();
    }

    private static FutureTask<String> futurePractice2() {
        FutureTask<String> value = new FutureTask<>(() -> {
            Thread.sleep(2000);
            System.out.println(getCurrentThread() + "Running!");
            return "Hello";
        }) {
            // 익명 클래스 생성 후 메서드 오버라이딩
            // 비동기 작업이 다 완료되면 실행되는 메서드이다.
            @Override
            protected void done() {
                // 여기서 리턴 값을 가져와보자.
                try {
                    System.out.println(getCurrentThread() + "Future = " + get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return value;
        /*
        [main] EXIT!
        [pool-1-thread-1] Running!
        [pool-1-thread-1] Future = Hello

        - 코드 자체는 깔끔하지 않지만... 아무튼 결과는 동일하게 나온다.
         */
    }

    private static FutureTask<String> futurePractice1() {
        // future는 단순히 결과만 리턴해주지만, Future Object를 받아올 때는 FutureTask를 사용한다.
        FutureTask<String> value = new FutureTask<>(() -> {
            Thread.sleep(2000);
            System.out.println(getCurrentThread() + "Running!");
            return "Hello";
        });
        return value;
        // 실행 결과 자체는 동일하다!
    }

    private static Future<String> submitPractice(ExecutorService es) {
        // submit의 경우 callable을 받을 수 있다. (리턴 가능) + 예외도 알아서 잡아준다.
        // execute의 경우 Runnable을 받기 때문에 리턴이 불가능하다.
        // Future를 이용하면 이렇게 리턴받은 값을 얘를 호출한 스레드에서 받아서 사용할 수 있다.
        Future<String> value = es.submit(() -> {
            Thread.sleep(2000);
            System.out.println(getCurrentThread() + "Running!");
            return "Hello";
        });
        return value;

        /*
        [pool-1-thread-1] Running!
        [main] Future = Hello
        [main] EXIT!

        - 다음과 같이 future의 작업이 끝나고 나서 실행된다.
        이런 과정이 'Blocking'되었다고 표현하는데, 특정 작업이 끝날 때까지 계속 대기 상태에 있음을 말한다.
        리턴이 된 이후에 값 출력 이후 EXIT가 실행된다.
        근데 사실 이렇게 되면 별도의 스레드에서 실행한 메리트가 사라져버린다...!

       그래도 EXIT의 위치를 future 값을 받아오기 전에 진행한다면
       병렬적으로 수행할 수 있기 때문에 다른 작업 + 비동기 작업을 동시에 수행해볼 수 있다.

        ======

        [main] isDone? = false
        [pool-1-thread-1] Running!
        [main] Future = Hello
        [main] EXIT!

        - 이런 식으로 완료되었는지 묻는 건 바로 출력이 된다.
         */
    }

    private static void executePractice(ExecutorService es) {
        es.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(getCurrentThread() + "Running!");
        });
       /*
        [main] EXIT!
        [pool-1-thread-1] Running!

        - 실행된 결과를 보면 메인 스레드는 이미 종료되고, 별도의 스레드에서 동작한 걸 볼 수 있다.
        */
    }
}
