package de.unimarburg.diz.termmapper.configuration;

import de.unimarburg.diz.termmapper.mapper.TerminologyMapper;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.util.ResourceHelper;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableConfigurationProperties
public class MappingConfiguration {

    @Bean
    public MappingProperties mappingProperties() {
        return new MappingProperties();
    }

    @Bean("mappingPackage")
    public Resource currentMappingFile(MappingProperties mp)
        throws IOException {
        return ResourceHelper.getMappingFile(mp
            .getPkg()
            .getVersion(), mp
            .getPkg()
            .getCredentials()
            .getUser(), mp
            .getPkg()
            .getCredentials()
            .getPassword(), mp
            .getPkg()
            .getProxy(), mp
            .getPkg()
            .getLocal());
    }

    @Bean
    public SwisslabMap swisslabMap(
        @Qualifier("mappingPackage") Resource pkg)
        throws IOException {
        return SwisslabMap.buildFromPackage(pkg);
    }

    @Bean("termMapper")
    public Function<Bundle, Bundle> termMapper(
        List<TerminologyMapper<Bundle>> mappers) {
        var pipeline = mappers.stream()
            .reduce(TerminologyMapper::chain)
            .orElse(TerminologyMapper.identity());

        return pipeline::apply;
    }
}
