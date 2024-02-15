FROM openjdk:21
LABEL authors="serafim"
WORKDIR /app

ENTRYPOINT ["top", "-b"]

COPY target/SearchEngine-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]