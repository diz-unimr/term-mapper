FROM eclipse-temurin:17.0.18_8-jdk-jammy AS build
WORKDIR /home/gradle/src
ENV GRADLE_USER_HOME=/gradle

COPY . .
RUN keytool -import -trustcacerts -cacerts -noprompt -storepass changeit \
    -alias rka_ca -file cert/RKA_CA.crt && \
    ./gradlew clean build --info && \
    java -Djarmode=layertools -jar build/libs/*.jar extract

FROM gcr.io/distroless/java17:debug-nonroot

USER nonroot
COPY --from=build /opt/java/openjdk/lib/security/cacerts /etc/ssl/certs/java/cacerts
WORKDIR /opt/term-mapper
COPY --from=build /home/gradle/src/dependencies/ ./
COPY --from=build /home/gradle/src/spring-boot-loader/ ./
COPY --from=build /home/gradle/src/application/ ./
COPY HealthCheck.java .

ARG GIT_REF=""
ARG GIT_URL=""
ARG BUILD_TIME=""
ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90", "org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD ["java", "HealthCheck.java", "||", "exit", "1"]

LABEL org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="Sebastian Stöcker" \
    org.opencontainers.image.source=${GIT_URL} \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="diz.uni-marburg.de" \
    org.opencontainers.image.title="term-mapper" \
    org.opencontainers.image.description="Kafka terminology mapping processor"
