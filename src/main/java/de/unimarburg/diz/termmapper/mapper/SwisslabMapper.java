package de.unimarburg.diz.termmapper.mapper;

import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

@Slf4j
@Getter
abstract class SwisslabMapper implements TerminologyMapper<Bundle> {

    private final FhirProperties fhirProperties;
    private final SwisslabMap mapping;

    protected SwisslabMapper(FhirProperties fhirProperties,
                             SwisslabMap mapping) {
        this.fhirProperties = fhirProperties;
        this.mapping = mapping;
    }

    protected String getSwisslabCode(Observation o) {
        return o
            .getCode()
            .getCoding()
            .stream()
            .filter(c -> getFhirProperties()
                .getSystems()
                .getLaboratorySystem()
                .equals(c.getSystem()))
            .map(Coding::getCode)
            .findAny().orElseThrow();
    }
}
