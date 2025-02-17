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
            .withSystem("http://loinc.org")
            .withUcum("mmol/L")
            .build());
        original.put("BAR", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withSystem("http://loinc.org")
            .withUcum("mmol/L")
            .build());

        var from = new SwisslabMap(null);
        from.put("FOO", new SwisslabMapEntry.Builder()
            .withCode("1000-0")
            .withSystem("http://loinc.org")
            .withUcum("mmol/L")
            .build());
        from.put("BAR", new SwisslabMapEntry.Builder()
            .withCode("1111-0")
            .withSystem("http://loinc.org")
            .withUcum("mmol/L")
            .build());

        var diff = original.diff(from);

        assertThat(diff).isEqualTo(Set.of("BAR"));
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    void putAddsCodesBySystemCodeAndMeta() {
        var map = new SwisslabMap(null);

        // loinc
        map.put("TEST", new SwisslabMapEntry.Builder()
            .withCode("loinc")
            .withSystem("loinc")
            .build());

        // loinc with meta
        map.put("TEST", new SwisslabMapEntry.Builder()
            .withCode("loinc")
            .withSystem("loinc")
            .withMeta("meta")
            .build());

        // snomed with different codes
        map.put("TEST", new SwisslabMapEntry.Builder()
            .withCode("one")
            .withSystem("snomed")
            .build());
        map.put("TEST", new SwisslabMapEntry.Builder()
            .withCode("two")
            .withSystem("snomed")
            .build());

        assertThat(map.getInternalMap().get("TEST")).hasSize(4);
    }
}
