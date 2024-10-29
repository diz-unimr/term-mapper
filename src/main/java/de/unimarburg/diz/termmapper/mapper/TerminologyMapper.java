package de.unimarburg.diz.termmapper.mapper;

import jakarta.validation.constraints.NotNull;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.hl7.fhir.r4.model.Resource;

import java.util.Objects;


public interface TerminologyMapper<T extends Resource>
    extends Comparable<TerminologyMapper<T>>, ValueMapper<T, T> {

    @NotNull
    default TerminologyMapper<T> chain(@NotNull TerminologyMapper<T> after) {
        Objects.requireNonNull(after);
        return (t) -> after.apply(this.apply(t));
    }

    static <T extends Resource> TerminologyMapper<T> identity() {
        return t -> t;
    }

    default int compareTo(@NotNull TerminologyMapper t) {
        return 0;
    }
}

