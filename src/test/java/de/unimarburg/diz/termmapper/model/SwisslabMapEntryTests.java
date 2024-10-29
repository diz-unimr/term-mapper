package de.unimarburg.diz.termmapper.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class SwisslabMapEntryTests {

    @Test
    public void entryIsEqual() {
        EqualsVerifier
            .forClass(SwisslabMapEntry.class)
            .verify();
    }

}
