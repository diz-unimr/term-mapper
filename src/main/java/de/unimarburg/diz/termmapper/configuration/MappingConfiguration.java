package de.unimarburg.diz.termmapper.configuration;

import de.unimarburg.diz.termmapper.mapper.TerminologyMapper;
import de.unimarburg.diz.termmapper.model.SwisslabMap;
import de.unimarburg.diz.termmapper.util.ResourceHelper;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
        return getMappingFile(mp
            .getLoinc()
            .getVersion(), mp
            .getLoinc()
            .getCredentials()
            .getUser(), mp
            .getLoinc()
            .getCredentials()
            .getPassword(), mp
            .getLoinc()
            .getProxy(), mp
            .getLoinc()
            .getLocal());
    }

    private Resource getMappingFile(
        @Value("${mapping.package.version}") String version,
        @Value("${mapping.package.credentials.user}") String user,
        @Value("${mapping.package.credentials.password}") String password,
        @Value("${mapping.package.proxy}") String proxyServer,
        @Value("${mapping.package.local}") String localPkg) throws IOException {

        return ResourceHelper.getMappingFile(version, user, password,
            proxyServer, localPkg);
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
