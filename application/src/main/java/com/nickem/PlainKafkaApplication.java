package com.nickem;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class PlainKafkaApplication {

    private static final String REQUEST_TOPIC = "file-management-requests-01-test_tenant_sftp";
    private static final String RESPONSE_TOPIC = "file-management-responses-01-test_tenant_sftp";
    private static final String BROKERS = "localhost:29092";

    private Consumer<Long, String> consumer;
    private Producer<Long, String> producer;

    public PlainKafkaApplication(Consumer<Long, String> consumer,
                                 Producer<Long, String> producer) {
        this.consumer = consumer;
        this.producer = producer;
    }

    public static void main(String[] args) throws Exception {
        Consumer<Long, String> consumer = createConsumer();
        Producer<Long, String> producer = createProducer();
        final PlainKafkaApplication instance = new PlainKafkaApplication(consumer, producer);
        instance.startProcessing();
    }

    private void startProcessing() throws Exception {
        while (true) {
            ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(5000));
            process(records);
            consumer.commitSync();
        }
    }

    private void process(final ConsumerRecords<Long, String> records) throws Exception {
        for (ConsumerRecord<Long, String> record : records) {
            log.info("Red record from consumer {}", record);

            final Long key = Instant.now().getEpochSecond();
            final String valueToProduce = String.format("%s-processed", record.value());
            final ProducerRecord<Long, String> produceRecord = new ProducerRecord<>(RESPONSE_TOPIC, key, valueToProduce);

            producer.send(produceRecord, new Callback() {
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    if (e != null) {
                        log.debug("Send failed for record {}", produceRecord, e);
                    } else {
                        log.debug("Record is send successfully!");
                    }
                }
            });
        }
    }

    private static Consumer<Long, String> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "fms-plain-kafka");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        final Consumer<Long, String> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Collections.singletonList(REQUEST_TOPIC));

        return consumer;
    }

    private static Producer<Long, String> createProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

}
