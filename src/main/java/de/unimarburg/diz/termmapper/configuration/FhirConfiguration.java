package de.unimarburg.diz.termmapper.configuration;

import ca.uhn.fhir.context.FhirContext;
import de.unimarburg.diz.termmapper.model.AppFhirContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class FhirConfiguration {


    @Bean
    public FhirContext fhirContext() {
        return AppFhirContext.getInstance();
    }

    @Bean
    public FhirProperties fhirProperties() {
        return new FhirProperties();
    }

}

