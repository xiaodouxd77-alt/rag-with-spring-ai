FROM eclipse-temurin:21-jdk
COPY target/rag-1.0.2.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]