package de.unimarburg.diz.termmapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class TermMapperApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
        SpringApplication.run(TermMapperApplication.class, args);
    }

}
