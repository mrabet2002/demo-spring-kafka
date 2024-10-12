package ma.enset.demospringkafka.web;

import lombok.RequiredArgsConstructor;
import ma.enset.demospringkafka.events.PageEvent;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PageEventRestController {
    private final StreamBridge streamBridge;
    private final InteractiveQueryService interactiveQueryService;

    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publishEvent(
            @PathVariable String topic,
            @PathVariable String name
    ) {
        var pageEvent = new PageEvent(name,
                Math.random() > 0.5 ? "user1" : "user2", new Date(),
                (long) (Math.random() * 1000));
        streamBridge.send(topic, pageEvent);
        return pageEvent;
    }

    @GetMapping(value = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> {
                            ReadOnlyWindowStore<String, Long> store = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore());
                            Instant now = Instant.now();
                            Instant from = now.minusSeconds(5);
                            KeyValueIterator<Windowed<String>, Long> iterator = store.fetchAll(from, now);

                            Map<String, Long> result = new HashMap<>();
                            while (iterator.hasNext()) {
                                KeyValue<Windowed<String>, Long> next = iterator.next();
                                result.put(next.key.key(), next.value);
                            }

                            return result;
                        }
                );
    }

    @GetMapping(value = "/analytics/{page}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics(@PathVariable String page) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> {
                            ReadOnlyWindowStore<String, Long> store = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore());
                            Instant now = Instant.now();
                            Instant from = now.minusSeconds(5);
                            Map<String, Long> result;
                            try (WindowStoreIterator<Long> iterator = store.fetch(page, from, now)) {

                                result = new HashMap<>();
                                while (iterator.hasNext()) {
                                    KeyValue<Long, Long> next = iterator.next();
                                    result.put(page, next.value);
                                }
                            }

                            return result;
                        }
                );
    }
}