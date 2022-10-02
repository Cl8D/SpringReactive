package study.reactiveStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
@Slf4j
/** Flux의 동작 방식 알아보기 */
public class ReactiveStreamApplication2 {

    /** Mono */
    @GetMapping("/event/{id}")
    Mono<Event> event(@PathVariable Long id) {
        return Mono.just(new Event(id, "Event " + id));

        // 결과) {"id":1,"value":"Event 1"}
    }

    /** Flux */
    // 약간 컬렉션처럼 여러 개의 결과를 처리하고 싶다면 Flux를 사용하기.
    // 보통 Mono의 경우 0~1개의 결과를 처리하지만, FLux의 경우 0~N개의 결과를 처리한다.
    @GetMapping("/events")
    Flux<Event> events() {
        // Flux의 경우 다음과 같이 여러 개를 넣어줄 수 있다.
        return Flux.just(new Event(1L, "event1"),
                new Event(2L, "event2"));

        // 결과) [{"id":1,"value":"event1"},{"id":2,"value":"event2"}]
    }

    /** List in Mono */
    @GetMapping("/event/list")
    Mono<List<Event>> eventList() {
        List<Event> list = Arrays.asList(new Event(1L, "event1"), new Event(2L, "event2"));
        // ** 여기서 만약 map을 걸게 되면 컬렉션 자체가 하나의 단위가 되기 때문에 컬렉션 단위로 stream이 동작하게 된다.
        return Mono.just(list);

        // 결과) [{"id":1,"value":"event1"},{"id":2,"value":"event2"}]
        // flux를 사용한 것과 동일한 결과가 나온다.
    }

    /** Flux - fromIterable */
    @GetMapping("/events/iterable")
    Flux<Event> eventsIterable() {
        List<Event> list = Arrays.asList(new Event(1L, "event1"), new Event(2L, "event2"));
        // 여기서는 바로 선언된 컬렉션을 넣어줄 수 없다. just에는 Event 객체 각각을 여러 개 넣어주는 게 가능하기 때문에 List 자체를 넣는 건 안 된다.
        // 그 대신, Flux의 fromIterable를 사용해서 넣어주면 된다.

        // ** 그러나, 여기서 map을 걸게 되면 각각의 event에 대한 가공이 가능하다.
        return Flux.fromIterable(list);
        // 결과) [{"id":1,"value":"event1"},{"id":2,"value":"event2"}]
        // 결과 자체는 동일!
    }

    /** Flux - fromIterable - produces; TEXT_EVENT_STREAM_VALUE */
    // produces의 경우 클라이언트의 요청에서 온 accept 헤더의 정보를 보고 매핑하는 목적으로 쓰이는데,
    // 혹은 리턴 타입에 대한 미디어 타입을 명시적으로 지정해주는 용도로 사용할 수 있다.
    @GetMapping(value = "/events/text-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventTextStream() {
        List<Event> list = Arrays.asList(new Event(1L, "event1"), new Event(2L, "event2"));

        return Flux.fromIterable(list);
        /*
            결과)
            data:{"id":1,"value":"event1"}
            data:{"id":2,"value":"event2"}
            - 이런 식으로 각 요소 각각을 받을 수 있게 된다!
            - array로 묶이지 않고 각각을 json으로 컨버팅된 값이 나온다.
         */
    }

    /** Flux - Data Generation */
    @GetMapping(value = "/events/data-gen", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventDataGen() {
        return Flux.fromStream(Stream.generate(() ->
                // generate가 호출될 때마다 데이터를 하나씩 만들어나가기
                new Event(System.currentTimeMillis(), "value!")))
                // 1초 정도의 딜레이 주기 - 딜레이를 처리하는 "별도의 스레드"를 통해서 10초 정도 잡고 있는다.
                .delayElements(Duration.ofSeconds(1))
                // 데이터는 10개만 가져오겠다는 의미. (무한 스트림 방지) - 개수가 차면 cancel()을 날려서 취소한다.
                .take(10);
        /*
            결과)
            data:{"id":1664725744389,"value":"value!"}
            data:{"id":1664725745408,"value":"value!"}
            data:{"id":1664725746416,"value":"value!"}
            data:{"id":1664725747424,"value":"value!"}
            data:{"id":1664725748431,"value":"value!"}
            ...

            - 이런 식으로 1초에 한 번씩 데이터가 날라온다.
         */
    }

    /** Data Generation - Advanced  */
    @GetMapping(value = "/events/data-gen2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventDataGen2() {
        // 자바에서 사용하는 stream을 통한 stream 대신 flux 자체를 이용해보자.
        return Flux
                // sink를 통해 다음 데이터를 생성해줄 수 있다.
                .<Event>generate(sink -> sink.next(new Event(System.currentTimeMillis(), "value!")))
                .delayElements(Duration.ofSeconds(1))
                .take(10);
        // 결과 자체는 처음 거랑 동일하게 나온다.
    }

    /** Data Generation using state */
    @GetMapping(value = "/events/data-gen3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventDataGen3() {
        return Flux
                // generate의 파라미터로 일종의 'state'를 넘겨줘서 초기에서 어떠한 상태가 되도록 코드 변경
                // 초기값, 상태값과 sink을 처리하는 람다식 정의
                // 타입 힌트를 줄 때 첫 번째 인자가 리턴 타입이고, 두 번째 인자가 초기 타입인 듯...?
                .<Event, Long>generate(() -> 1L, (id, sink) -> {
                    sink.next(new Event(id, "value! " + id));
                    // 바뀐 상태를 리턴해주기
                    return id+1;
                })
                .delayElements(Duration.ofSeconds(1))
                .take(10);
        /*
            결과)
            data:{"id":1,"value":"value! 1"}
            data:{"id":2,"value":"value! 2"}
            data:{"id":3,"value":"value! 3"}
            data:{"id":4,"value":"value! 4"}
            ...

            - 이런 식으로 이전의 결과값을 바탕으로 다음값을 만들어내는 것을 볼 수 있다.
         */
    }

    /** Data Generation using interval with zip */
    @GetMapping(value = "/events/data-gen4", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventDataGen4() {
        Flux<Event> flux = Flux.<Event, Long>generate(() -> 1L, (id, sink) -> {
                    sink.next(new Event(id, "value! " + id));
                    return id + 1;
                })
                .take(10);

        // interval을 통해 데이터 생성 시 일정한 주기를 줄 수 있다.
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        // 둘을 연결해주자. zip 내부에 두 개의 stream을 넣어준다.
        // tuple 처럼 첫 번째 event에 첫 번째 interval... 이런 식으로 묶어준다.
        // tuple.getT1()을 통해 첫 번째에 해당하는 데이터만 가져올 수 있다.
        return Flux.zip(flux, interval).map(tuple -> tuple.getT1());

        /*
            결과) getT1()
            data:{"id":1,"value":"value! 1"}
            data:{"id":2,"value":"value! 2"}
            data:{"id":3,"value":"value! 3"}
            ...

            - 이전이랑 동일한 결과가 나온다.

            결과) getT2()
            data:0
            data:1
            data:2
            ...

            - 커지는 값이 나오는데, 0부터 시작해서 9까지 나온다.
            - 당연하게도 두 번째는 값을 생성하는 것만 하고, 실질적인 결과값 생성은 return id+1 이 과정에서 일어나기 때문에
            return id+2 를 해도 동일하게 0~9까지 10개가 생성된다!
         */
    }

    /** data-generation4의 다른 버전 */
    @GetMapping(value = "/events/data-gen5", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventDataGen5() {
        Flux<String> flux = Flux.generate(sink -> sink.next("value!"));
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        // Event 생성을 여기에서 해줘도 된다.
        return Flux.zip(flux, interval)
                .map(tuple -> new Event(tuple.getT2()+1, tuple.getT1())).take(10);
    }

        @Data
    @AllArgsConstructor
    public static class Event {
        Long id;
        String value;
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveStreamApplication2.class, args);
    }
}
