package chapter1;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("deprecation")
public class ObserverTest {

    /**
     * 1. 옵저버 패턴에게는 complete가 없다.
     * 2. 코드가 동작하다 오류가 발생하게 된다면 예외 전파 방식, 에외 처리 방식에 대해 어려워진다.
     */

    public static void main(String[] args) {
        // iterable 때문에 deprecate된 친구...
        Observer observer1 = new Observer() {
            // 일종의 subscriber
            // notifyObservers 동작 시 실행된다.
            // 즉, observable이 던진 데이터를 받아서 출력해주는 역할!
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("observer1 = " + arg);
            }
        };

        Observer observer2 = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("observer2 = " + arg);
            }
        };

        Observer observer3 = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("observer3 = " + arg);
            }
        };


        IntObservable io = new IntObservable();
        // observer를 등록해준다. 이러면 observable이 던진 작업을 등록된 옵저버들이 받는다.
        // 실행 순서의 경우 가장 마지막에 등록해준 옵저버부터 순차적으로 실행되는 것 같다.
        io.addObserver(observer1);
        io.addObserver(observer2);
        io.addObserver(observer3);
        /*
        observer3 = 1
        observer2 = 1
        observer1 = 1
        observer3 = 2
        observer2 = 2
        observer1 = 2
        ...
         */


        // Executors를 통해서 ExecutorService 생성 가능
        // 스레드를 새롭게 생성해준 다음에 할당해주기
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(io);

        // execute보다 나중에 입력했는데도 더 먼저 실행된다.
        // excute는 스레드를 새롭게 생성해서 (별개의 스레드) 동작하기 때문에 더 나중에 동작하게 되는 것
        System.out.println("Hello!");
        es.shutdown();
    }

    // observable은 외부에서 이벤트를 만들어내야 한다.
    // 비동기로 실행하기 위해서 runnable 구현
    static class IntObservable extends Observable implements Runnable {
        @Override
        public void run() {
            for(int i=1; i<=10; i++) {
                setChanged(); // 변화가 생겼음을 알려주기 위해서, Observable에 등록되어 있음.
                // 일종의 Push 느낌. 사실 iter에서의 it.next()와 비슷한 느낌이다.
                notifyObservers(i); // 옵저버에게 알려주기!, 일종의 publisher
            }
        }
    }
}
