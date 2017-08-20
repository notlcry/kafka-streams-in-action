package bbejeck.chapter_7;


import bbejeck.chapter_6.transformer.StockPerformanceTransformer;
import bbejeck.clients.producer.MockDataProducer;
import bbejeck.model.StockPerformance;
import bbejeck.model.StockTransaction;
import bbejeck.util.serde.StreamsSerdes;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.state.Stores;

import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.processor.TopologyBuilder.AutoOffsetReset.LATEST;

public class StockPerformanceStreamsAndProcessorApplication {


    public static void main(String[] args) throws Exception {


        StreamsConfig streamsConfig = new StreamsConfig(getProperties());
        Serde<String> stringSerde = Serdes.String();
        Serde<StockPerformance> stockPerformanceSerde = StreamsSerdes.StockPerformanceSerde();
        Serde<StockTransaction> stockTransactionSerde = StreamsSerdes.StockTransactionSerde();


        StreamsBuilder builder = new StreamsBuilder();

        String stocksStateStore = "stock-performance-store";
        double differentialThreshold = 0.05;

        builder.addStateStore(Stores.create(stocksStateStore).withStringKeys()
                .withValues(stockPerformanceSerde).inMemory().maxEntries(100).build());

        builder.stream(stringSerde, stockTransactionSerde, "stock-transactions")
                .transform(()-> new StockPerformanceTransformer(stocksStateStore, differentialThreshold), stocksStateStore)
                //.print(stringSerde, stockPerformanceSerde, "StockPerformance")
                //Uncomment this line and comment out the line above for writing to a topic
                .to(stringSerde, stockPerformanceSerde, "stock-performance");


        KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsConfig);
        MockDataProducer.produceStockTransactionsWithKeyFunction(50, 50, 25, StockTransaction::getSymbol);
        System.out.println("Stock Analysis KStream/Process API App Started");
        kafkaStreams.cleanUp();
        kafkaStreams.start();

        System.out.println(kafkaStreams.metrics());


        Thread.sleep(70000);


        System.out.println("Shutting down the Stock KStream/Process API Analysis App now");
        for (Map.Entry<MetricName, ? extends Metric> metricNameEntry : kafkaStreams.metrics().entrySet()) {
            Metric metric = metricNameEntry.getValue();
            MetricName metricName = metricNameEntry.getKey();
            if(metric.value() != 0.0 && metric.value() != Double.NEGATIVE_INFINITY) {
                System.out.print("MetricName " + metricName.name());
                System.out.println(" = " + metric.value());
            }   

        }
        kafkaStreams.close();
        MockDataProducer.shutdown();
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "ks-stats-stock-analysis-client");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ks-stats-stock-analysis-group");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ks-stats-stock-analysis-appid");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        return props;
    }
}