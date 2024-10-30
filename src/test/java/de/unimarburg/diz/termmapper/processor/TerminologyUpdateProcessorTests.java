package de.unimarburg.diz.termmapper.processor;

import de.unimarburg.diz.termmapper.configuration.FhirConfiguration;
import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.configuration.MappingConfiguration;
import de.unimarburg.diz.termmapper.mapper.LoincMapper;
import de.unimarburg.diz.termmapper.mapper.TerminologyMapper;
import de.unimarburg.diz.termmapper.model.MapperOffsets;
import de.unimarburg.diz.termmapper.model.MappingInfo;
import de.unimarburg.diz.termmapper.model.MappingUpdate;
import de.unimarburg.diz.termmapper.processor.TerminologyUpdateProcessorTests.KafkaConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.test.TestRecord;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TerminologyUpdateProcessor.class,
    TerminologyMapper.class,
    LoincMapper.class, FhirConfiguration.class, MappingConfiguration.class,
    KafkaConfig.class})
@TestPropertySource(properties = {"mapping.loinc.version=''",
    "mapping.loinc.credentials.user=''",
    "mapping.loinc.credentials.password=''",
    "mapping.loinc.local=mapping-swl-loinc.zip"})
public class TerminologyUpdateProcessorTests extends BaseProcessorTests {

    @Autowired
    private TerminologyUpdateProcessor processor;


    @Autowired
    private FhirProperties fhirProperties;

    @SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:LineLength"})
    @Test
    void updateIsProcessed() {

        // build stream
        try (var driver = buildStream(processor.update())) {

            var labTopic = createInputTopic(driver);
            var outputTopic = createOutputTopic(driver);

            // NA values are updated, but update only processes the first two
            // because the last one (4) is where the default processor picks up
            // according to processOffsets()
            var inputReports =
                List.of(createBundle("1", "NA"), createBundle("2", "ERY"),
                    createBundle("3", "NA"), createBundle("4", "NA"));

            // create input records
            labTopic.pipeKeyValueList(inputReports.stream()
                .map(r -> new KeyValue<>(r.getId(), r))
                .toList());

            // get record from output topic
            var outputRecords = outputTopic.readRecordsToList();

            // expected keys are: 1, 3
            assertThat(outputRecords.stream().map(TestRecord::getKey)
                .toList()).isEqualTo(List.of("1", "3"));

            // assert codes are mapped
            var obsCodes = getObservationsCodes(outputRecords).toList();

            // all updated observations have LOINC coding for NA
            assertThat(obsCodes).allMatch(
                codes -> codes.hasCoding("http://loinc.org", "2951-2"));
        }
    }

    private Bundle createBundle(String bundleId, String labCode) {
        var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent()
                .setResource(new DiagnosticReport().addIdentifier(
                    new Identifier().setValue(bundleId))))
            .addEntry(new Bundle.BundleEntryComponent()
                .setResource(new Observation().setCode(
                    new CodeableConcept().addCoding(new Coding()
                        .setSystem(
                            fhirProperties.getSystems().getLaboratorySystem())
                        .setCode(labCode))).setValue(new Quantity(1.0))));

        bundle.setId(bundleId);
        return bundle;
    }

    @TestConfiguration
    static class KafkaConfig {

        @SuppressWarnings("checkstyle:MagicNumber")
        @Bean
        MapperOffsets testOffsets() {
            // offset target will be 3 on partition 0
            return new MapperOffsets(Map.of(0, new OffsetAndMetadata(3L)),
                Map.of());
        }

        @Bean
        MappingInfo testMappingInfo() {
            return new MappingInfo(new MappingUpdate(null, null, List.of("NA")),
                false);
        }
    }
}
