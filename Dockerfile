# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

# Run
FROM eclipse-temurin:21-jre
WORKDIR /work
COPY --from=build /app/target/*-runner.jar /work/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /work/app.jar"]