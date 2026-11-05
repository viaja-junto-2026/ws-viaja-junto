FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# O lint (Checkstyle) tem job próprio no CI; a imagem não precisa repeti-lo.
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -Dcheckstyle.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]