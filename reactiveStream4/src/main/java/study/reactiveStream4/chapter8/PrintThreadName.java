package study.reactiveStream4.chapter8;

public class PrintThreadName {
    public static String getCurrentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}
