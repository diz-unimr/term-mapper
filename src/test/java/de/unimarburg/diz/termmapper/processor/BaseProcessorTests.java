package de.unimarburg.diz.termmapper.processor;

import de.unimarburg.diz.termmapper.serializer.FhirDeserializer;
import de.unimarburg.diz.termmapper.serializer.FhirSerde;
import de.unimarburg.diz.termmapper.serializer.FhirSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.test.TestRecord;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:LineLength")
abstract class BaseProcessorTests {

    Stream<CodeableConcept> getObservationsCodes(
        List<TestRecord<String, Bundle>> records) {
        return records
            .stream()
            .flatMap(b -> b
                .getValue()
                .getEntry()
                .stream()
                .map(BundleEntryComponent::getResource))
            .filter(Observation.class::isInstance)
            .map(Observation.class::cast)
            .map(Observation::getCode);
    }

    TestInputTopic<String, Bundle> createInputTopic(
        TopologyTestDriver driver) {
        return driver.createInputTopic("lab", new StringSerializer(),
            new FhirSerializer<>());
    }

    TestOutputTopic<String, Bundle> createOutputTopic(
        TopologyTestDriver driver) {
        return driver.createOutputTopic("lab-mapped", new StringDeserializer(),
            new FhirDeserializer<>(Bundle.class));
    }

    TopologyTestDriver buildStream(
        Function<KStream<String, Bundle>, KStream<String, Bundle>> processor) {
        var builder = new StreamsBuilder();
        final KStream<String, Bundle> labStream = builder.stream(
            "lab",
            Consumed.with(Serdes.String(),
                new FhirSerde<>(Bundle.class)));

        processor
            .apply(labStream)
            .to("lab-mapped", Produced.with(Serdes.String(),
                Serdes.serdeFrom(new FhirSerializer<>(),
                    new FhirDeserializer<>(Bundle.class))));

        return new TopologyTestDriver(builder.build());
    }

    Bundle createReport(int reportId, Coding labCoding) {
        var bundle = new Bundle();
        bundle.setId(String.valueOf(reportId));
        bundle.addEntry().setResource(new DiagnosticReport()
            .addIdentifier(new Identifier().setValue("report-id"))
            .setSubject(new Reference(
                new Patient().addIdentifier(new Identifier().setValue("1"))))
            .setEncounter(new Reference(new Encounter().addIdentifier(
                new Identifier().setValue("1")))));

        var obs = new Observation()
            .addIdentifier(new Identifier().setValue("obs-id"))
            .setCode(new CodeableConcept().setCoding(List.of(labCoding)))
//            .setValue(new Quantity(1));
            .setValue(new StringType("bla"));
        obs.setId("obs-id");
        bundle.addEntry().setResource(obs);

        return bundle;
    }
}
