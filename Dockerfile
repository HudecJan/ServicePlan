FROM eclipse-temurin:17-jdk-alpine AS build
RUN apk add --no-cache dos2unix
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Clear local maven.config (corporate proxy settings not needed in Docker)
RUN echo "" > .mvn/maven.config && dos2unix mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -q
COPY src src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/servicePlan-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
