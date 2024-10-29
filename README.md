# term-mapper
[![MegaLinter](https://github.com/diz-unimr/term-mapper/actions/workflows/mega-linter.yml/badge.svg?branch=main)](https://github.com/diz-unimr/term-mapper/actions/workflows/mega-linter.yml?query=branch%3Amain) ![java](https://github.com/diz-unimr/term-mapper/actions/workflows/build.yml/badge.svg) ![docker](https://github.com/diz-unimr/term-mapper/actions/workflows/release.yml/badge.svg) [![codecov](https://codecov.io/gh/diz-unimr/term-mapper/branch/main/graph/badge.svg?token=ub0ZDTKwrz)](https://codecov.io/gh/diz-unimr/term-mapper)

> Kafka FHIR🔥 terminology mapping processor

The processor maps terminologies like LOINC, UCUM and SNOMED CT from local
codings and values.

It currently supports parsing laboratory data from FHIR resources
(i.e. Observation as FHIR Bundle entrie resources).

## Mappings

- Observation.code to LOINC
- Observation.valueQuantity to UCUM
- Observation.valueString to Observation.valueCodeableConcept via SNOMED CT

### LOINC mapping

Observation resources which have numerical result values are mapped to LOINC and UCUM using a fixed mapping.

On startup, the processor loads data from a [mapping package](https://gitlab.diz.uni-marburg.de/mapping/loinc-mapping/-/packages) which consists of a csv file and metadata.

This data is looked up on processing and results in an additional `coding` (`"system": "http://loinc.org"`) where a LOINC mapping entry exists.
The original Swisslab coding is kept in either case.

Result quantities for value and references ranges are mapped to their corresponding UCUM units.

## <a name="deploy_config"></a> Configuration

The following environment variables can be set:

| Variable                           | Default                             | Description                                                                                                                                                                                                                                  |
|------------------------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BOOTSTRAP_SERVERS                  | localhost:9092                      | Kafka brokers                                                                                                                                                                                                                                |
| SECURITY_PROTOCOL                  | PLAINTEXT                           | Kafka communication protocol                                                                                                                                                                                                                 |
| SSL_TRUST_STORE_LOCATION_INTERNAL  | /opt/lab-to-fhir/ssl/truststore.jks | Truststore location                                                                                                                                                                                                                          |
| SSL_TRUST_STORE_PASSWORD           |                                     | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                       |
| SSL_KEY_STORE_LOCATION_INTERNAL    | /opt/lab-to-fhir/ssl/keystore.jks   | Keystore location                                                                                                                                                                                                                            |
| SSL_KEY_STORE_PASSWORD             |                                     | Keystore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                         |
| SSL_TRUST_STORE_PASSWORD           |                                     | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                                                                                                                                       |
| INPUT_TOPIC                        | lab-fhir                            | Topic to read FHIR input data from                                                                                                                                                                                                           |
| OUTPUT_TOPIC                       | lab-fhir-term                       | Topic to store mapped result bundles                                                                                                                                                                                                         |
| MAPPING_LOINC_VERSION              | 4.0.0                               | LOINC mapping package version: [Package Registry · mapping / loinc-mapping](https://gitlab.diz.uni-marburg.de/mapping/loinc-mapping/-/packages/))                                                                                            |
| MAPPING_LOINC_CREDENTIALS_USER     |                                     | LOINC mapping package registry user                                                                                                                                                                                                          |
| MAPPING_LOINC_CREDENTIALS_PASSWORD |                                     | LOINC mapping package registry password                                                                                                                                                                                                      |
| MAPPING_LOINC_PROXY                |                                     | Proxy server to use when pulling the package                                                                                                                                                                                                 |
| MAPPING_LOINC_LOCAL                |                                     | Name of the local LOINC mapping package file to use (see [application resources](src/main/resources)) <br /><br /> **NOTE**: This option does not pull the file from the registry and credentials and version are fixed by the local package |
| LOG_LEVEL                          | info                                | Log level (error, warn, info, debug)                                                                                                                                                                                                         |

Additional application properties can be set by overriding values form the [application.yml](src/main/resources/application.yml) by using environment variables.

## Mapping updates

In addition to the regular Kafka processor this application uses a separate
update processor to apply mapping updates to all records up until the
current offset state of the regular processor.

The update processor is a separate Kafka consumer and keeps its own offset
state in order to be able to resume unfinished updates. On completion, the
update consumer group is deleted.

On startup, the application checks the configured mapping version and
determines a diff between the mappings of the current and the last used
mapping version. This data is stored in the Kafka topic `mapping` with the key
`lab-update`.

In case there are no changes or the mapping versions used are equal, the
update processor is not started.

## Error handling

### Serialization errors

Errors which occur during serialization of records from the input topic cause the processor to stop
and move to an error state.

### Mapping errors

Records which can't be mapped are skipped.

## Deployment

This project includes a docker compose file for deployment purposes.
Environment variables can be set according to the
provided `sample.env`. Remember to replace the `IMAGE_TAG` variable according to the desired version tag. Available
tags can be found at the [Container Registry](https://github.com/orgs/diz-unimr/packages?repo_name=lab-to-fhir) or under [Releases](https://github.com/diz-unimr/lab-to-fhir/releases).

## Development

A [test setup](dev/compose.yaml) with test data is available for development
purposes.

### Builds

You can build a docker image for this processor by using the provided [Dockerfile](Dockerfile).

⚠ FHIR profiles must be installed for the build step to run successfully.

## License

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
