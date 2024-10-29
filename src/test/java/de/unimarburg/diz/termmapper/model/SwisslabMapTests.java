package de.unimarburg.diz.termmapper.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SwisslabMapTests {

    @Test
    void diffCreatesCodeDiff() {
        var original = new SwisslabMap(null);
        original.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withUcum("mmol/L")
            .build());
        original.put("BAR", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withUcum("mmol/L")
            .build());

        var from = new SwisslabMap(null);
        from.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withUcum("mmol/L")
            .build());
        from.put("BAR", new SwisslabMapEntry.Builder()
            .withCode("1111-0")
            .withUcum("mmol/L")
            .build());

        var diff = original.diff(from);

        assertThat(diff).isEqualTo(Set.of("BAR"));
    }

    @Test
    void bla() {
        var map = new SwisslabMap(null);
        map.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withSystem("http://loinc.org")
            .withUcum("mmol/L")
            .build());
        map.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000-1")
            .withSystem("http://loinc.org")
            .withMeta("TEST")
            .withUcum("mmol/L")
            .build());
        map.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000000")
            .withSystem("http://snomed.org")
            .withTextValue("bla")
            .build());

        assertThat(map.size()).isEqualTo(3);
    }

}
