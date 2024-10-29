package de.unimarburg.diz.termmapper.mapper;

import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.model.SwisslabMapEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SnomedMapper extends SwisslabMapper {

    private static final String SYSTEM = "http://snomed.org";

    protected SnomedMapper(
        FhirProperties fhirProperties,
        SwisslabMap mapping) {
        super(fhirProperties, mapping);
    }

    @Override
    public Bundle apply(Bundle bundle) {

        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
            .filter(Observation.class::isInstance)
            .map(Observation.class::cast).forEach(this::map);

        return bundle;
    }

    void map(Observation obs) {

        if (!obs.hasValueStringType()) {
            return;
        }

        var swlCode = getSwisslabCode(obs);

        var mappings = getMapping().get(swlCode, SYSTEM, null);
        if (mappings.isEmpty()) {
            return;
        }

        mapEntries(obs, mappings);
    }

    private void mapEntries(Observation obs, List<SwisslabMapEntry> entries) {
        var value = obs.getValueStringType().getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        // find first matching
        var mapping =
            entries.stream()
                .filter(m -> value.equals(m.getTextValue()))
                .findAny();
        if (mapping.isEmpty()) {
            return;
        }

        log.debug("Found mapping for code: {} with SNOMED: {}",
            entries.get(0).getSwl(),
            entries.get(0).getCode());

        // set value to snomed coding
        var snomedCoding = new Coding()
            .setSystem("http://snomed.info/sct")
            .setCode(mapping.get().getCode())
            .setVersion(mapping.get().getVersion())
            .setDisplay(value);

        obs.setValue(new CodeableConcept().addCoding(snomedCoding));
    }
}
