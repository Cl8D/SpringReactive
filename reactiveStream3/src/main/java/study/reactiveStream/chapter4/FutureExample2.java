package study.reactiveStream.chapter4;

import java.util.Objects;
import java.util.concurrent.*;

import static study.reactiveStream.chapter4.PrintThreadName.getCurrentThread;

/** Java Future Practice 2 */
public class FutureExample2 {

    /** 이전의 futurePractice2() 메서드와 동일한 작업을 하지만 코드상으로 좀 더 깔끔하게 처리할 수 있다. */
    // 비동기 작업이 정상 수행되었다면 해당 작업을 담는 인터페이스
    interface SuccessCallback {
        void onSuccess(String result);
    }

    // 추가적으로 예외가 발생했을 때 메인 스레드에서 처리할 수 있도록 담아주는 인터페이스를 정의하자.
    interface ExceptionCallback {
        void onError(Throwable t);
    }

    public static class CallbackFutureTask extends FutureTask<String> {
        SuccessCallback sc;
        ExceptionCallback ec;

        public CallbackFutureTask(Callable<String> callable, SuccessCallback sc, ExceptionCallback ec) {
            super(callable);
            // null이 아니면 값이 들어가고, null이면 NPE 터짐
            this.sc = Objects.requireNonNull(sc);
            this.ec = Objects.requireNonNull(ec);
        }

        // 작업이 완료했을 때 호출
        @Override
        protected void done() {
            try {
                sc.onSuccess(get());
            } catch (InterruptedException e) {
                // interruptException의 경우 작업 종료가 아닌 작업 종료를 하라는 메시지를 던지기 때문에,
                // 현재 스레드에게 인터럽트가 일어났다는 시그널을 전달한다.
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // 비동기 작업을 수행하다가 예외가 발생했을 경우
                ec.onError(e.getCause()); // 예외 값 자체를 전달하기
            }
        }
    }

    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();

        // 생성자로 callable (수행할 비동기 작업)과 SuccessCallback의 구현체를 넣어준다. (성공 시 결과값을 넣어줄 콜백)
        CallbackFutureTask task = new CallbackFutureTask(() -> {
            Thread.sleep(2000);
            if (1==1)
                throw new RuntimeException("Error!!!");

            System.out.println(getCurrentThread() + "Running!");
            return "Hello";
        }, success ->
            // 작업이 성공했을 때 할 태스크 넣기
            System.out.println(getCurrentThread() + "Result = " + success)
        , error ->
            System.out.println(getCurrentThread() + "Error = " + error.getMessage())
        );
        es.execute(task);
        System.out.println(getCurrentThread() + "EXIT!");
        es.shutdown();

        /*
        <Success>
        [main] EXIT!
        [pool-1-thread-1] Running!
        [pool-1-thread-1] Result = Hello

        - 이렇게 이전과 동일한 결과가 나오는 걸 볼 수 있다.

        <Error>
        [main] EXIT!
        [pool-1-thread-1] Error = Error!!!

        - 다음과 같이 에러 메시지가 깔끔하게 잘 나오는 걸 볼 수 있다.
         */
    }
}
