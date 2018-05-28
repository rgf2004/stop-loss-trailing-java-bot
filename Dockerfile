FROM openjdk:8-jre-alpine
COPY ./target/core-0.0.1-SNAPSHOT.jar /usr/src/
WORKDIR /usr/src/
VOLUME /usr/src/application.properties
CMD ["java", "-jar", "core-0.0.1-SNAPSHOT.jar"]