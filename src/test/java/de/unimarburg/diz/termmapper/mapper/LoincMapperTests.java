package de.unimarburg.diz.termmapper.mapper;

import de.unimarburg.diz.termmapper.configuration.FhirConfiguration;
import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.configuration.MappingProperties;
import de.unimarburg.diz.termmapper.mapper.LoincMapperTests.LoincMapperTestConfiguration;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.model.SwisslabMapEntry;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationReferenceRangeComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {FhirConfiguration.class, MappingProperties.class,
    LoincMapperTestConfiguration.class})
public class LoincMapperTests {

    private static SwisslabMap testMap;
    @Autowired
    private LoincMapper loincMapper;

    @Autowired
    private FhirProperties fhirProperties;

    @Autowired
    private MappingProperties mappingProperties;

    @SuppressWarnings("checkstyle:LineLength")
    private static Stream<Arguments> mapCodeAndQuantityProvidesMetaCodeArguments() {
        return Stream.of(Arguments.of("TEST", List.of(), List.of("1000-0")),
            Arguments.of("TEST", List.of("meta1"), List.of("1000-0", "1000-1")),
            Arguments.of("TEST", List.of("meta2"), List.of("1000-0", "1000-2")),
            // fallback to null source
            Arguments.of("TEST", List.of("not-mapped"), List.of("1000-0")),
            Arguments.of("UNKNOWN", List.of(), List.of()));
    }

    private static Stream<Arguments> ucumSourceIsCheckedArguments() {
        return Stream.of(
            // mapped
            Arguments.of(new Quantity().setCode("old unit"), "mmol/L"),
            Arguments.of(new Quantity().setCode("mmol/L"), "mmol/L"),
            // not mapped
            Arguments.of(new Quantity().setCode("mmol/l"), "mmol/l"));
    }

    @BeforeAll
    public static void init() {
        testMap = new SwisslabMap(null);
        var testCode = "TEST";
        testMap.put(testCode, new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withSystem("http://loinc.org")
            .withSwlUnit("old unit")
            .withUcum("mmol/L")
            .build());
        testMap.put(testCode, new SwisslabMapEntry.Builder()
            .withMeta("meta1")
            .withCode("1000-1")
            .withSystem("http://loinc.org")
            .withSwlUnit("old unit")
            .withUcum("mmol/L")
            .build());
        testMap.put(testCode, new SwisslabMapEntry.Builder()
            .withMeta("meta2")
            .withCode("1000-2")
            .withSystem("http://loinc.org")
            .withSwlUnit("old unit")
            .withUcum("mmol/L")
            .build());
    }

    @ParameterizedTest
    @MethodSource("mapCodeAndQuantityProvidesMetaCodeArguments")
    public void mapCodeAndQuantityProvidesMetaCode(String code,
                                                   List<String> metaCodes,
                                                   List<String> expectedCodes) {
        var obs = new Observation().setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(fhirProperties
                    .getSystems()
                    .getLaboratorySystem())
                .setCode(code))).setValue(new Quantity());
        metaCodes.forEach(
            m -> obs.getCode().addCoding(new Coding().setCode(m)));

        loincMapper.map(obs, metaCodes);

        // swisslab code is preserved
        assertThat(obs.getCode().hasCoding(fhirProperties
            .getSystems()
            .getLaboratorySystem(), code)).isTrue();
        // loinc mappings
        assertThat(obs
            .getCode()
            .getCoding().stream().filter(o -> Objects.equals(o.getSystem(),
                "http://loinc.org")).map(Coding::getCode).toList())
            .isEqualTo(expectedCodes);
    }

    @Test
    public void ucumUnitsAreMapped() {
        // arrange
        var oldUnitQuantity = new Quantity().setUnit("old unit");
        var obs = new Observation()
            .setCode(new CodeableConcept().addCoding(new Coding(fhirProperties
                .getSystems()
                .getLaboratorySystem(), "TEST", null)))
            .setValue(oldUnitQuantity)
            .addReferenceRange(new ObservationReferenceRangeComponent()
                .setLow(oldUnitQuantity)
                .setHigh(oldUnitQuantity));

        // act
        loincMapper.map(obs, null);

        // assert value has new code and old (human readable) unit
        assertThat(obs.getValueQuantity().getCode()).isEqualTo("mmol/L");
        assertThat(obs.getValueQuantity().getUnit()).isEqualTo("old unit");
        // .. as well as reference range unit and code
        assertThat(obs.getReferenceRange())
            .flatExtracting(x -> x
                .getLow()
                .getCode(), x -> x
                .getHigh()
                .getCode())
            .containsOnly("mmol/L");
    }

    @ParameterizedTest
    @MethodSource("ucumSourceIsCheckedArguments")
    public void ucumSourceIsChecked(Quantity quantity, String expectedUnit) {
        mappingProperties.setVerifyUnits(true);

        // arrange
        var obs = new Observation()
            .setCode(new CodeableConcept().addCoding(new Coding(fhirProperties
                .getSystems()
                .getLaboratorySystem(), "TEST", null)))
            .setValue(quantity)
            .addReferenceRange(new ObservationReferenceRangeComponent()
                .setLow(quantity)
                .setHigh(quantity));

        // act
        loincMapper.map(obs, null);

        // assert
        assertThat(obs.getValueQuantity().getCode()).isEqualTo(expectedUnit);
    }

    @TestConfiguration
    static class LoincMapperTestConfiguration {

        @Bean
        public LoincMapper loincMapper(FhirProperties fhirProperties,
                                       MappingProperties mappingProperties) {
            return new LoincMapper(fhirProperties, mappingProperties, testMap);
        }
    }
}
