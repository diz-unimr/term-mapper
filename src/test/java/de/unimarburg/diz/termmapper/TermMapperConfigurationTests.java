package de.unimarburg.diz.termmapper;

import de.unimarburg.diz.termmapper.processor.TerminologyProcessor;
import de.unimarburg.diz.termmapper.processor.TerminologyUpdateProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9092", "port=9092"})
@SpringBootTest
@TestPropertySource(properties = {"mapping.pkg.version=''",
    "mapping.pkg.credentials.user=''",
    "mapping.pkg.credentials.password=''",
    "mapping.pkg.local=mapping-swl-loinc.zip",
    "spring.cloud.stream.kafka.streams.binder.replicationFactor=1",
    "spring.cloud.stream.kafka.streams.binder.minPartitionCount=1",
    "spring.cloud.stream.bindings.process-in-0.destination=lab"})
public class TermMapperConfigurationTests {

    @Autowired
    private TerminologyProcessor labProcessor;

    @Autowired
    private TerminologyUpdateProcessor updateProcessor;

    @Test
    void contexLoads() {
        assertThat(labProcessor).isNotNull();
        assertThat(updateProcessor).isNotNull();
    }

}
