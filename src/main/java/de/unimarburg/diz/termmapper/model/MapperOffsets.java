package de.unimarburg.diz.termmapper.model;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;

import java.util.Map;

public record MapperOffsets(Map<Integer, OffsetAndMetadata> processOffsets,
                            Map<Integer, OffsetAndMetadata> updateOffsets) {

}
