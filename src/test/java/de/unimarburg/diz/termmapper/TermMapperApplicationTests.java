package de.unimarburg.diz.termmapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:LineLength")
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9092", "port=9092"})
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    properties = {
        "mapping.loinc.local=mapping-swl-loinc.zip",
        "spring.cloud.stream.kafka.streams.binder.replicationFactor=1",
        "spring.cloud.stream.kafka.streams.binder.minPartitionCount=1"})
public class TermMapperApplicationTests {

    @Test
    public void contextLoads() {
        assertThat(TimeZone.getDefault()).isEqualTo(
            TimeZone.getTimeZone("Europe/Berlin"));
    }
}
