package de.unimarburg.diz.termmapper.configuration;

import de.unimarburg.diz.termmapper.model.MapperOffsets;
import de.unimarburg.diz.termmapper.model.MappingInfo;
import de.unimarburg.diz.termmapper.model.MappingUpdate;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.util.ResourceHelper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Configuration
public class MappingUpdateConfiguration {

    private static final Logger LOG =
        LoggerFactory.getLogger(MappingUpdateConfiguration.class);
    private static final long MAX_CONSUMER_POLL_DURATION_SECONDS = 10L;

    @SuppressWarnings("checkstyle:LineLength")
    @Bean("mappingInfo")
    public MappingInfo buildMappingUpdate(SwisslabMap swisslabMap,
                                          MappingProperties mappingProperties,
                                          MapperOffsets labOffsets,
                                          Consumer<String, MappingUpdate> consumer,
                                          Producer<String, MappingUpdate> producer,
                                          @Value("${spring.cloud.stream.bindings.process-in-0.destination}")
                                          String key)
        throws ExecutionException, InterruptedException, IOException {
        // check versions
        var configuredVersion = swisslabMap.getMetadata().getVersion();

        // 1. consume latest from (mapping) update topic
        var lastUpdate = getLastMappingUpdate(consumer);
        if (lastUpdate == null) {
            // job runs the first time: save current state
            lastUpdate = new MappingUpdate(configuredVersion, null, List.of());
            try {
                saveMappingUpdate(producer, lastUpdate, key);
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to save mapping update to topic.");
                throw e;
            }

        }

        // 2. check if already on latest
        if (Objects.equals(configuredVersion, lastUpdate.getVersion())) {
            LOG.info(
                "Configured mapping version ({}) matches last version ({}). "
                    + "No update necesssary", configuredVersion,
                lastUpdate.getVersion());

            if (!labOffsets.updateOffsets().isEmpty()) {
                // update in progress, continue
                return new MappingInfo(lastUpdate, true);
            }

            return null;
        }

        // 3. calculate diff of mapping versions and save new update
        // get last version's mapping
        var lastMap = SwisslabMap.buildFromPackage(
            ResourceHelper.getMappingFile(lastUpdate.getVersion(),
                mappingProperties.getPkg().getCredentials().getUser(),
                mappingProperties.getPkg().getCredentials().getPassword(),
                mappingProperties.getPkg().getProxy(),
                mappingProperties.getPkg().getLocal()));
        // ceate diff
        var updates = swisslabMap.diff(lastMap);
        var update =
            new MappingUpdate(configuredVersion, lastUpdate.getVersion(),
                updates);

        // save new mapping update
        saveMappingUpdate(producer, update, key);

        return new MappingInfo(update, false);
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void saveMappingUpdate(Producer<String, MappingUpdate> producer,
                                   MappingUpdate mappingUpdate, String key)
        throws ExecutionException, InterruptedException {

        producer.send(
                new ProducerRecord<>("mapping", key, mappingUpdate))
            .get();
    }

    private MappingUpdate getLastMappingUpdate(
        Consumer<String, MappingUpdate> consumer) {
        var topic = "mapping";

        var partition = new TopicPartition(topic, 0);
        var partitions = List.of(partition);

        // TODO read latest by key

        try (consumer) {
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
            var position = consumer.position(partition);
            if (position == 0) {
                return null;
            }

            consumer.seek(partition, position - 1);

            var record = consumer
                .poll(Duration.ofSeconds(MAX_CONSUMER_POLL_DURATION_SECONDS))
                .iterator().next();

            consumer.unsubscribe();
            return record.value();
        }
    }
}
