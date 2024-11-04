package de.unimarburg.diz.termmapper.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

@Getter
@Setter
@ConfigurationProperties(prefix = "mapping")
public class MappingProperties implements Serializable {

    private final Package pkg = new Package();
    private boolean verifyUnits;

    @Getter
    @Setter
    public static class Package implements Serializable {

        private final Credentials credentials = new Credentials();
        private String version;
        private String proxy;
        private String local;

        @Getter
        @Setter
        public static class Credentials implements Serializable {

            private String user;
            private String password;
        }
    }

}
