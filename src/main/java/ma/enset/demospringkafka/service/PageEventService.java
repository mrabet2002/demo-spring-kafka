package ma.enset.demospringkafka.service;

import ma.enset.demospringkafka.events.PageEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class PageEventService {
    @Bean
    public Consumer<PageEvent> pageEventConsumer() {
        return pageEvent -> {
            System.out.println("PageEvent: " + pageEvent);
            System.out.println("---------------------------------");
        };
    }

    @Bean
    public Supplier<PageEvent> pageEventSupplier() {
        return () -> {
            var pageEvent = new PageEvent(generateRandomName(),
                    Math.random() > 0.5 ? "user1" : "user2", new Date(),
                    (long) (Math.random() * 1000));
            System.out.println("PageEvent: " + pageEvent);
            System.out.println("---------------------------------");
            return pageEvent;
        };
    }

    private String generateRandomName() {
        String[] names = {"home", "search", "product", "checkout", "payment"};
        return names[(int) (Math.random() * names.length)];
    }

    @Bean
    Function<KStream<String, PageEvent>, KStream<String, Long>> kStreamFunction() {
        return kStream -> kStream
                .filter((k, v) -> v.getDuration() > 300)
                .map((k, v) -> new KeyValue<>(v.getName(), 0L))
                .groupBy((k,v)->k,Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
                .count(Materialized.as("page-count"))
                .toStream()
                .map((k,v)->new KeyValue<>(k.key()+" => ["+k.window().startTime()+"-"+k.window().endTime()+"]",v));
    }
}
