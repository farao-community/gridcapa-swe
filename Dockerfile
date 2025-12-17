FROM farao/farao-computation-base:1.9.0 AS BUILDER
ARG JAR_FILE=gridcapa-swe-runner-app/target/*.jar
COPY ${JAR_FILE} app.jar
RUN mkdir -p /tmp/app  \
    && java -Djarmode=tools  \
    -jar /app.jar extract --layers --launcher \
    --destination /tmp/app

FROM farao/farao-computation-base:1.9.0
COPY --from=BUILDER /tmp/app/dependencies/ ./
COPY --from=BUILDER /tmp/app/spring-boot-loader/ ./
COPY --from=BUILDER /tmp/app/application/ ./
COPY --from=BUILDER /tmp/app/snapshot-dependencies/ ./
ENTRYPOINT ["java", "-cp", "BOOT-INF/lib/*:BOOT-INF/classes", "com.farao_community.farao.swe.runner.app.SweApplication"]