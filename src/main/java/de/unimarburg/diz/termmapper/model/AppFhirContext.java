package de.unimarburg.diz.termmapper.model;

import ca.uhn.fhir.context.FhirContext;

public class AppFhirContext {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    public static FhirContext getInstance() {
        return FHIR_CONTEXT;
    }
}
