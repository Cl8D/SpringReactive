package chapter1;

import java.util.Iterator;

public class Basic {
    public static void main(String[] args) {
        // Iterable을 구현한 구현체 생성
        Iterable<Integer> iter = () ->
                // Iterator라는 도구를 custom 하기
                new Iterator<Integer>() {
                    // 1~10까지 생성
                    int i = 0;
                    final static int MAX = 10;

                    @Override
                    public boolean hasNext() {
                        return i < MAX;
                    }

                    @Override
                    public Integer next() {
                        return ++i; // 다음 값 가져오기
                    }
                };

        // custom이지만 for-each 구문을 사용할 수 있다는 것. (enhanced for)
        // 즉, 자바에서 for-each는 Iterable의 구현체에서 사용이 가능하다.
        for(Integer i : iter) {
            System.out.println(i);
        }

        // Java 10 버전
        for(Iterator<Integer> it = iter.iterator(); it.hasNext();) {
            // pull의 느낌, 값을 가져오는 것.
            System.out.println(it.next());
        }
    }
}


