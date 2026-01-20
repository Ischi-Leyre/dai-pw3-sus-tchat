FROM eclipse-temurin:21-jre
WORKDIR /app
LABEL authors="ischi & leyre"

# COPY Repeat for each file that must be present in image
COPY target/SUS-TEAM-1.0-SNAPSHOT.jar /app/SUS-TEAM.jar

EXPOSE 8080

# ENTRYPOINT runs the Java app
ENTRYPOINT ["java", "-jar", "/app/SUS-TEAM.jar"]

# Default argument (can be overridden)
CMD []