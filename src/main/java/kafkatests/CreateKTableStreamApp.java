package kafkatests;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class CreateKTableStreamApp
{
    public static void main( String[] args )
    {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final Serde<String> stringSerde = Serdes.String();
        final KStream<String, String> stream = streamsBuilder.stream("kafka-tests.tasks-in", Consumed.with(stringSerde, stringSerde));
        final KTable<String, String> table = stream.toTable(Named.as("kafka-tests.tasks-store"));
        table.toStream().to("kafka-tests.tasks-out", Produced.with(stringSerde, stringSerde));
        final Topology topology = streamsBuilder.build();

        final Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-tasks");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final KafkaStreams streams = new KafkaStreams(topology, config);
        final CountDownLatch latch = new CountDownLatch(1);


        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook"){
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });


        try {
            streams.cleanUp();
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
