FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# O lint (Checkstyle) tem job próprio no CI; a imagem não precisa repeti-lo.
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -Dcheckstyle.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Roda sem privilégios de administrador: um problema na aplicação não vira acesso root ao contêiner.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

EXPOSE 8080

# Usa o wget do Alpine (busybox) para consultar o Actuator, que já é público em /actuator/health.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
