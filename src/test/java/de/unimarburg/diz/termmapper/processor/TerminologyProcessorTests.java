package de.unimarburg.diz.termmapper.processor;

import de.unimarburg.diz.termmapper.configuration.FhirConfiguration;
import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.configuration.MappingConfiguration;
import de.unimarburg.diz.termmapper.mapper.LoincMapper;
import de.unimarburg.diz.termmapper.mapper.SnomedMapper;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {
    TerminologyProcessor.class,
    SnomedMapper.class, LoincMapper.class, FhirConfiguration.class,
    MappingConfiguration.class})
@TestPropertySource(properties = {"mapping.loinc.version=''",
    "mapping.loinc.credentials.user=''",
    "mapping.loinc.credentials.password=''",
    "mapping.loinc.local=mapping-swl-loinc.zip"})

public class TerminologyProcessorTests extends BaseProcessorTests {

    @Autowired
    private TerminologyProcessor processor;


    @Autowired
    private FhirProperties fhirProperties;

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void observationIsLoincMapped() {
        // build stream
        try (var driver = buildStream(processor.process())) {

            var labTopic = createInputTopic(driver);
            var outputTopic = createOutputTopic(driver);

            var labReport = createReport(42, new Coding()
                .setSystem(fhirProperties
                    .getSystems()
                    .getLaboratorySystem())
                .setCode("NA"));

            // create input record
            labTopic.pipeInput(String.valueOf(labReport.getId()), labReport);

            // get record from output topic
            var outputRecords = outputTopic.readRecordsToList();

            var obsCodes = getObservationsCodes(outputRecords)
                .findAny()
                .orElseThrow();

            // assert both codings exist
            assertThat(
                obsCodes.hasCoding("http://loinc.org", "2951-2")).isTrue();
            assertThat(obsCodes.hasCoding(fhirProperties
                .getSystems()
                .getLaboratorySystem(), "NA")).isTrue();
        }
    }
}
