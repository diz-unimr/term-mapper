package de.unimarburg.diz.termmapper.configuration;

import de.unimarburg.diz.termmapper.model.CsvPackageMetadata;
import de.unimarburg.diz.termmapper.model.MapperOffsets;
import de.unimarburg.diz.termmapper.model.MappingInfo;
import de.unimarburg.diz.termmapper.model.MappingUpdate;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MappingConfiguration.class)
@TestPropertySource(properties = {"mapping.pkg.version=''",
    "mapping.pkg.credentials.user=''",
    "mapping.pkg.credentials.password=''",
    "mapping.pkg.local=mapping-swl-loinc.zip"})
public class MappingUpdateConfigurationTests {

    @Autowired
    private MappingConfiguration mappingConfig;

    private static Stream<Arguments> buildMappingUpdateChecksLastUpdateArgs() {
        return Stream.of(
            // first run: no mapping update
            Arguments.of(
                Named.named("no mapping update", null), false,
                Named.named("first run",
                    null)),
            // no update: versions match, no offsets
            Arguments.of(Named.named("versions match, no offsets",
                    new MappingUpdate("2.0.0", "1.0.0",
                        null)), false,
                Named.named("no update", null)),
            // update: versions don't match, no offsets
            Arguments.of(
                Named.named("versions don't match, no offsets",
                    new MappingUpdate("1.0.0", null, null)), false,
                Named.named("update",
                    new MappingInfo(new MappingUpdate("2.0.0", "1.0.0",
                        Collections.emptyList()), false))),
            // update (in progress): versions match, offsets exist
            Arguments.of(
                Named.named("versions match, offsets exist",
                    new MappingUpdate("2.0.0", "1.0.0", null)), true,
                Named.named("update (in progress)",
                    new MappingInfo(new MappingUpdate("2.0.0", "1.0.0",
                        Collections.emptyList()), true))),
            // manual update: versions match, manual is set
            Arguments.of(
                Named.named("versions match, manual=true",
                    new MappingUpdate(
                        "2.0.0",
                        "2.0.0", null, true)), false,
                Named.named("manual update",
                    new MappingInfo(new MappingUpdate("2.0.0", "2.0.0",
                        Collections.emptyList()), false)))
        );
    }

    @ParameterizedTest
    @MethodSource("buildMappingUpdateChecksLastUpdateArgs")
    void buildMappingUpdateChecksLastUpdate(MappingUpdate latestUpdate,
                                            boolean offsetsExist,
                                            MappingInfo expected)
        throws IOException, ExecutionException, InterruptedException {

        // setup metadata and end offsets for mapping topic
        var metadata = new CsvPackageMetadata();
        var map = new SwisslabMap(metadata.setVersion("2.0.0"));
        var offsets = new MapperOffsets(null, offsetsExist ? Map.of(1,
            new OffsetAndMetadata(1)) : Map.of());

        // mock consumer / producer
        try (var consumer = createMockConsumer(latestUpdate);
             var producer = new MockProducer<String, MappingUpdate>(true,
                 new StringSerializer(), new JsonSerializer<>())) {

            var info =
                new MappingUpdateConfiguration().buildMappingUpdate(map,
                    mappingConfig.mappingProperties(),
                    offsets,
                    consumer,
                    producer, "lab");

            assertThat(info).usingRecursiveComparison()
                .ignoringFields("update.updates").isEqualTo(expected);
        }
    }

    private MockConsumer<String, MappingUpdate> createMockConsumer(
        MappingUpdate latestUpdate) {
        var consumer = new MockConsumer<String, MappingUpdate>(
            OffsetResetStrategy.EARLIEST);
        // setup topic and partition
        var partitions = Collections.singletonList(new TopicPartition("mapping",
            0));
        var endOffsets =
            partitions.stream().collect(Collectors
                .toMap(Function.identity(),
                    tp -> latestUpdate == null ? 0L : 1L));

        // set end offsets for seek to end to work
        consumer.updateEndOffsets(endOffsets);

        if (latestUpdate != null) {
            // add latestUpdate record
            consumer.schedulePollTask(() -> consumer.addRecord(
                new ConsumerRecord<>("mapping", 0, 1L, "lab",
                    latestUpdate)));
        }

        return consumer;
    }

}
