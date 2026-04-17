FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
