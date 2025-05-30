package de.unimarburg.diz.termmapper.mapper;

import de.unimarburg.diz.termmapper.configuration.FhirProperties;
import de.unimarburg.diz.termmapper.configuration.MappingProperties;
import de.unimarburg.diz.termmapper.metric.TagCounter;
import de.unimarburg.diz.termmapper.model.MetaCode;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.model.SwisslabMapEntry;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LoincMapper extends SwisslabMapper {

    private static final Set<String> META_CODES = EnumSet.allOf(MetaCode.class)
        .stream()
        .map(Enum::toString)
        .collect(Collectors.toSet());
    private final TagCounter unmappedCodes;
    private static final String SYSTEM = "http://loinc.org";
    private final MappingProperties mappingProps;

    @Autowired
    public LoincMapper(FhirProperties fhirProperties,
                       MappingProperties mappingProps, SwisslabMap swlMap) {
        super(fhirProperties, swlMap);
        this.mappingProps = mappingProps;
        this.unmappedCodes = new TagCounter("loinc_unmapped",
            "Number of unmapped resources by code", "swl_code",
            Metrics.globalRegistry);
    }

    Observation map(Observation obs, List<String> metaCodeValues) {

        var swlCode = getSwisslabCode(obs);

        var mappings = getMapping().get(swlCode, SYSTEM, metaCodeValues);
        if (mappings.isEmpty()) {
            unmappedCodes.increment(swlCode);
            return obs;
        }

        return mapEntries(obs, mappings);
    }

    private Observation mapEntries(Observation obs,
                                   List<SwisslabMapEntry> entries) {

        log.debug("Found mapping for code: {} with LOINC: {}",
            entries.get(0).getSwl(),
            entries.get(0).getCode());

        entries.forEach(e -> {

                // add loinc coding
                var loincCoding = new Coding()
                    .setSystem(SYSTEM)
                    .setCode(e.getCode())
                    .setVersion(e.getVersion());
                obs
                    .getCode()
                    .getCoding()
                    .add(0, loincCoding);

                // map ucum in value and referenceRange(s)
                if (e.getUcum() == null || !obs.hasValueQuantity()) {
                    // no ucum mapping exists or value is no quantity
                    return;
                }

                // verify source unit
                if (mappingProps.isVerifyUnits()
                    && !(e.getUcum().equals(obs.getValueQuantity().getCode())
                    ||
                    e.getSwlUnit().equals(obs.getValueQuantity().getCode()))) {
                    // no ucum mapping exists or swl units don't match
                    log.warn("Swisslab unit ({}) doesn't match source or "
                            + "target unit from UCUM mapping: {} -> {}",
                        obs.getValueQuantity().getCode(), e.getSwlUnit(),
                        e.getUcum());
                    return;
                }

                // keep (human-readable) unit and map code to UCUM
                obs
                    .getValueQuantity()
                    .setCode(e.getUcum())
                    .setSystem("http://unitsofmeasure.org");
                obs
                    .getReferenceRange()
                    .forEach(quantity -> {
                        if (quantity.hasLow()) {
                            quantity
                                .getLow()
                                .setCode(e.getUcum())
                                .setSystem("http://unitsofmeasure.org");
                        }
                        if (quantity.hasHigh()) {
                            quantity
                                .getHigh()
                                .setCode(e.getUcum())
                                .setSystem("http://unitsofmeasure.org");
                        }
                    });
            }
        );

        return obs;
    }

    @Override
    public Bundle apply(Bundle bundle) {

        // select possible meta value(s)
        var metaCodeValues =
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .filter(o -> META_CODES.contains(getSwisslabCode(o))
                    && o.hasValueStringType())
                .map(this::parseMetaCodeValue).findAny().orElse(null);

        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
            .filter(Observation.class::isInstance)
            .map(Observation.class::cast).forEach(o -> map(o, metaCodeValues));

        return bundle;
    }

    private List<String> parseMetaCodeValue(Observation o) {
        var obsValue = o.getValueStringType().getValue();

        // handle special case of meta code value in notes
        if ("s.Bem.".equals(obsValue)) {
            return o.getNote().stream().map(Annotation::getText).toList();
        }

        return List.of(obsValue);
    }
}
