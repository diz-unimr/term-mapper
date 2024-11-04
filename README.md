# term-mapper
[![MegaLinter](https://github.com/diz-unimr/term-mapper/actions/workflows/mega-linter.yml/badge.svg?branch=main)](https://github.com/diz-unimr/term-mapper/actions/workflows/mega-linter.yml?query=branch%3Amain) ![java](https://github.com/diz-unimr/term-mapper/actions/workflows/build.yml/badge.svg) ![docker](https://github.com/diz-unimr/term-mapper/actions/workflows/release.yml/badge.svg) [![codecov](https://codecov.io/gh/diz-unimr/term-mapper/branch/main/graph/badge.svg?token=ub0ZDTKwrz)](https://codecov.io/gh/diz-unimr/term-mapper)

> Kafka FHIR🔥 terminology mapping processor

The processor maps terminologies like LOINC, UCUM and SNOMED CT from local
codings and values.

It currently supports parsing laboratory data from FHIR resources
(i.e. Observation as FHIR Bundle entry resources).

## Terminology mappings

On startup, the term-mapper loads data from a
[mapping package](https://gitlab.diz.uni-marburg.de/mapping/loinc-mapping/-/packages)
which consists of a csv file and metadata.

Observation resource values are then mapped using a fixed mapping between
terminologies.

## LOINC

A LOINC coding is added to the existing Swisslab coding if a mapping exists.
This can result in multiple new codings where a more specific as well as a
general mapping exists.

## UCUM

Observations with numerical result values (quantitative data) are mapped to
UCUM replacing the unit (`valueQuantity.system`, `valueQuantity.code`) but
keeping the original one (`valueQuantity.unit`) as a human-readable form of the
unit.

Reference range units are mapped accordingly.

## SNOMED CT

Textual result values are mapped to SNOMED CT replacing
`Observation.valueString` with `Observation.valueCodeableConcept`.

## <a name="deploy_config"></a> Configuration

The following environment variables can be set:

| Variable                          | Default                             | Description                                                                                                                                                                                                                            |
|-----------------------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BOOTSTRAP_SERVERS                 | localhost:9092                      | Kafka brokers                                                                                                                                                                                                                          |
| SECURITY_PROTOCOL                 | PLAINTEXT                           | Kafka communication protocol                                                                                                                                                                                                           |
| SSL_TRUST_STORE_LOCATION_INTERNAL | /opt/term-mapper/ssl/truststore.jks | Truststore location                                                                                                                                                                                                                    |
| SSL_TRUST_STORE_PASSWORD          |                                     | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                 |
| SSL_KEY_STORE_LOCATION_INTERNAL   | /opt/term-mapper/ssl/keystore.jks   | Keystore location                                                                                                                                                                                                                      |
| SSL_KEY_STORE_PASSWORD            |                                     | Keystore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                   |
| SSL_TRUST_STORE_PASSWORD          |                                     | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                 |
| INPUT_TOPIC                       | lab-fhir                            | Topic to read FHIR input data from                                                                                                                                                                                                     |
| OUTPUT_TOPIC                      | lab-fhir-term                       | Topic to store mapped result bundles                                                                                                                                                                                                   |
| CONSUMER_CONCURRENCY              | 1                                   | Number of concurrent consumers (processing and update each). Also determines the number of partitions for the output topic                                                                                                             |
| MAPPING_PKG_VERSION               | 4.0.0                               | LOINC mapping package version: [Package Registry · mapping / loinc-mapping](https://gitlab.diz.uni-marburg.de/mapping/loinc-mapping/-/packages/))                                                                                      |
| MAPPING_PKG_CREDENTIALS_USER      |                                     | LOINC mapping package registry user                                                                                                                                                                                                    |
| MAPPING_PKG_CREDENTIALS_PASSWORD  |                                     | LOINC mapping package registry password                                                                                                                                                                                                |
| MAPPING_PKG_PROXY                 |                                     | Proxy server to use when pulling the package                                                                                                                                                                                           |
| MAPPING_PKG_LOCAL                 |                                     | Name of the local mapping package file to use (see [application resources](src/main/resources)) <br /><br /> **NOTE**: This option does not pull the file from the registry and credentials and version are fixed by the local package |
| MAPPING_VERIFYUNITS               | false                               | Set to _true_ to only map UCUM units if they match source or target units from the mapping                                                                                                                                             |
| LOG_LEVEL                         | info                                | Log level (error, warn, info, debug)                                                                                                                                                                                                   |

Additional application properties can be set by overriding values form the [application.yml](src/main/resources/application.yml) by using environment variables.

## Mapping updates

In addition to the regular Kafka processor this application uses a separate
update processor to apply mapping updates to all records up until the
current offset state of the regular processor.

The update processor is a separate Kafka consumer/producer and keeps its own
offset state in order to be able to resume unfinished updates. On completion, the
update consumer group is deleted.

On startup, the application checks the configured mapping version and
determines a diff between the mappings of the current and the last used
mapping version. This data is stored in the Kafka topic `mapping` with the key
of the input topic name.

In case there are no changes or the mapping versions used are equal, the
update processor is not started.

## Error handling

### Serialization errors

Errors which occur during serialization of records from the input topic cause the processor to stop
and move to an error state.

### Mapping errors

Records which can't be mapped are skipped.

## Development

A [test setup](dev/compose.yaml) with test data is available for development
purposes.

### Builds

You can build a docker image for this processor by using the provided [Dockerfile](Dockerfile).

## License

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
