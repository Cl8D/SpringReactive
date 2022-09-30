package study.reactiveStream.chapter4;

public class PrintThreadName {
    public static String getCurrentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}
