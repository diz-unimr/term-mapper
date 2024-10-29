package de.unimarburg.diz.termmapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@JsonDeserialize(builder = SwisslabMapEntry.Builder.class)
public final class SwisslabMapEntry {

    private final String swl;
    private final String swlUnit;
    private final String code;
    private final String system;
    private final String version;
    private final String ucum;
    private final String meta;
    private final String textValue;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SwisslabMapEntry(String swl, String swlUnit, String code,
                             String system,
                             String version,
                             String ucum, String meta, String textValue) {
        this.swl = swl;
        this.swlUnit = swlUnit;
        this.code = code;
        this.system = system;
        this.version = version;
        this.ucum = ucum;
        this.meta = meta;
        this.textValue = textValue;
    }

    @JsonPOJOBuilder
    public static class Builder {

        @JsonProperty("SWL_CODE")
        private String swl;
        @JsonProperty("SWL_UNIT")
        private String swlUnit;
        @JsonProperty("CODE_VALUE")
        private String code;
        @JsonProperty("CODE_SYSTEM")
        private String system;
        @JsonProperty("VERSION")
        private String version;
        @JsonProperty("UCUM_UNIT")
        private String ucumUnit;
        @JsonProperty("META_CODE_VALUE")
        private String metaCode;
        @JsonProperty("TEXT_VALUE")
        private String textValue;

        public Builder withSwl(String swl) {
            this.swl = swl;
            return this;
        }

        public Builder withSwlUnit(String swlUnit) {
            this.swlUnit = swlUnit;
            return this;
        }

        public Builder withCode(String code) {
            this.code = code;
            return this;
        }

        public Builder withSystem(String system) {
            this.system = system;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withUcum(String ucum) {
            this.ucumUnit = ucum;
            return this;
        }

        public Builder withMeta(String meta) {
            this.metaCode = meta;
            return this;
        }

        public Builder withTextValue(String textValue) {
            this.textValue = textValue;
            return this;
        }


        public SwisslabMapEntry build() {
            return new SwisslabMapEntry(swl, swlUnit,
                code, system, version, ucumUnit,
                metaCode, textValue);
        }
    }
}
