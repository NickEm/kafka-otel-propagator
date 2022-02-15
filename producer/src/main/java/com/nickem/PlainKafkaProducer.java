package com.nickem;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class PlainKafkaProducer {

    private static final String REQUEST_TOPIC = "file-management-requests-01-test_tenant_sftp";
    private static final String BROKERS = "localhost:29092";

    private Producer<Long, String> producer;

    public PlainKafkaProducer(Producer<Long, String> producer) {
        this.producer = producer;
    }

    public static void main(String[] args) throws Exception {
        Producer<Long, String> producer = createProducer();
        final PlainKafkaProducer instance = new PlainKafkaProducer(producer);
        instance.produce();
    }

    private void produce() throws ExecutionException, InterruptedException {
        final Long key = Instant.now().getEpochSecond();
        final String value = String.format("test-%s", key);
        final ProducerRecord<Long, String> produceRecord = new ProducerRecord<>(REQUEST_TOPIC, key, value);

        final Future<RecordMetadata> send = producer.send(produceRecord, new Callback() {
            public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e != null) {
                    log.info("Send failed for record {}", produceRecord, e);
                } else {
                    log.info("Record is send successfully!");
                }
            }
        });

        send.get();
    }

    private static Producer<Long, String> createProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

}
