FROM eclipse-temurin:21-jdk@sha256:98155457fcdbeb0987472e8abf721657b698ede407aad52ac24ae42618c0931c AS build
WORKDIR /app
COPY gradle gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre@sha256:c8e00005c840f4e15a98dee36caf8b7be1e385350d63f4eb4a91e465bffa98a1
WORKDIR /app
RUN mkdir -p /data /comunidades && apt-get update -qq && apt-get install -y -qq curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/afinavila-all.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl -f http://localhost:8081/health || exit 1
ENV DB_PATH=/data/afinavila.db
ENV FILES_PATH=/comunidades
ENTRYPOINT ["java", "-jar", "app.jar"]
