package de.unimarburg.diz.termmapper.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "fhir")
@Validated
public class FhirProperties {

    private final Systems systems = new Systems();

    @Setter
    @Getter
    public static class Systems {

        @NotNull
        private String laboratorySystem;
        @NotNull
        private String laboratoryUnitSystem;


    }
}
