package de.unimarburg.diz.termmapper.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.kstream.KStream;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Slf4j
@Service
public class TerminologyProcessor {

    private final Function<Bundle, Bundle> termMapper;

    public TerminologyProcessor(@Qualifier("termMapper") Function<Bundle,
        Bundle> termMapper) {
        this.termMapper = termMapper;
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Bean
    public Function<KStream<String, Bundle>, KStream<String, Bundle>> process() {

        return bundle -> bundle.mapValues(termMapper::apply);
    }
}
