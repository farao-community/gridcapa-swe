FROM farao/farao-computation-base:1.9.0 AS builder
ARG JAR_FILE=gridcapa-swe-runner-app/target/*.jar
COPY ${JAR_FILE} app.jar
RUN mkdir -p /tmp/app  \
    && java -Djarmode=tools  \
    -jar /app.jar extract --layers --launcher \
    --destination /tmp/app

FROM farao/farao-computation-base:1.9.0
COPY --from=builder /tmp/app/dependencies/ ./
COPY --from=builder /tmp/app/spring-boot-loader/ ./
COPY --from=builder /tmp/app/application/ ./
COPY --from=builder /tmp/app/snapshot-dependencies/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]