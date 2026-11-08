FROM eclipse-temurin:21.0.13_9-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
WORKDIR /app
RUN groupadd --system geng && useradd --system --gid geng geng
COPY bootstrap/target/geng-backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER geng
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
